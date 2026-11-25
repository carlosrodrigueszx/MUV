package com.muv.backend.service;

import com.muv.backend.repository.AuthRepository;
import com.muv.backend.repository.ModalidadesDemonstracao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnvioSolicitacaoService {
    @Value("${muv.demo.dispensar-verificacao:false}")
    private boolean dispensarVerificacao;

    private final JdbcTemplate db;
    private final AuthRepository accounts;

    public EnvioSolicitacaoService(JdbcTemplate db, AuthRepository accounts) {
        this.db = db;
        this.accounts = accounts;
    }

    public record Endereco(
            @NotBlank @Pattern(regexp = "[0-9]{5}-?[0-9]{3}") String cep,
            @NotBlank @Size(max = 128) String logradouro,
            @NotBlank @Size(max = 20) String numero,
            @NotBlank @Size(max = 128) String bairro,
            @NotBlank @Size(max = 128) String cidade,
            @NotBlank @Pattern(regexp = "(?i)[a-z]{2}") String estado) {}

    public record Envio(
            @NotNull UUID chave,
            @Positive long prestadorId,
            @Positive long veiculoId,
            @Positive int passageiros,
            @NotNull LocalDateTime partida,
            LocalDateTime retorno,
            @NotNull @Valid Endereco origem,
            @NotNull @Valid Endereco destino,
            @NotNull @Size(max = 50) List<@NotNull @Valid Endereco> paradas,
            @DecimalMin("0.01") @Digits(integer = 11, fraction = 2) BigDecimal valor,
            @Size(max = 4000) String condicoes) {}

    @Transactional
    public long enviar(String email, Envio envio) {
        long passageiro = accounts.findAccount(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)).id();
        if (db.query("SELECT id FROM passageiro WHERE id = ? FOR UPDATE",
                (rs, row) -> rs.getLong(1), passageiro).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta conta não tem perfil de passageiro.");
        }

        var anterior = db.query("SELECT id FROM solicitacao_frete WHERE id_passageiro = ? AND chave_envio = ?",
                (rs, row) -> rs.getLong(1), passageiro, envio.chave());
        if (!anterior.isEmpty()) return anterior.getFirst();

        if (passageiro == envio.prestadorId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha outro prestador.");
        }
        if (!envio.partida().isAfter(LocalDateTime.now(ZoneId.of("America/Fortaleza")))
                || (envio.retorno() != null && !envio.retorno().isAfter(envio.partida()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confira os horários de ida e retorno.");
        }
        String condicoes = envio.condicoes() == null || envio.condicoes().isBlank() ? null : envio.condicoes().trim();
        if ((envio.valor() == null) != (condicoes == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o valor e as condições da oferta juntos.");
        }

        String consultaVeiculo = """
                SELECT v.id FROM veiculo v
                JOIN autorizacao_modalidade a ON a.id_prestador = v.id_prestador
                WHERE v.id = ? AND v.id_prestador = ? AND v.ativo
                  AND v.modalidade IN ('PESSOA', 'AMBOS') AND v.capacidade_passageiros >= ?
                  AND a.modalidade = 'PESSOA' AND a.status = 'APROVADO'
                """;
        String bloqueio = dispensarVerificacao ? " FOR SHARE OF v" : " FOR SHARE OF v, a";
        var elegiveis = db.query(ModalidadesDemonstracao.aplicar(consultaVeiculo, dispensarVerificacao) + bloqueio,
                (rs, row) -> rs.getLong(1), envio.veiculoId(), envio.prestadorId(), envio.passageiros());
        if (elegiveis.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O veículo deixou de ser compatível. Consulte os resultados novamente.");
        }

        long origem = gravarEndereco(envio.origem());
        long destino = gravarEndereco(envio.destino());
        long id = db.queryForObject("""
                INSERT INTO solicitacao_frete (modalidade, data_hora_pretendida, retorno_previsto, status,
                    id_passageiro, id_prestador, endereco_origem_id, endereco_destino_id,
                    valor_cliente, condicoes_cliente, chave_envio)
                VALUES ('PESSOA', ?, ?, 'ABERTA', ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, envio.partida(), envio.retorno(), passageiro, envio.prestadorId(),
                origem, destino, envio.valor(), condicoes, envio.chave());
        db.update("INSERT INTO solicitacao_pessoa (id, quantidade_pessoas) VALUES (?, ?)", id, envio.passageiros());
        int ordem = 1;
        for (Endereco parada : envio.paradas()) {
            db.update("INSERT INTO solicitacao_parada (id_solicitacao, ordem, endereco_id) VALUES (?, ?, ?)",
                    id, ordem++, gravarEndereco(parada));
        }
        db.update("""
                INSERT INTO notificacao (titulo, mensagem, usuario_destino_id, solicitacao_id)
                VALUES ('Nova solicitação direcionada', 'Consulte o roteiro e responda à solicitação.', ?, ?)
                """, envio.prestadorId(), id);
        return id;
    }

    private long gravarEndereco(Endereco endereco) {
        return db.queryForObject("""
                INSERT INTO endereco (cep, logradouro, numero, bairro, cidade, estado)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, endereco.cep(), endereco.logradouro().trim(), endereco.numero().trim(),
                endereco.bairro().trim(), endereco.cidade().trim(), endereco.estado().toUpperCase(Locale.ROOT));
    }
}
