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
    private final NotificacaoInternaService notificacoes;

    public EnvioSolicitacaoService(JdbcTemplate db, AuthRepository accounts,
            NotificacaoInternaService notificacoes) {
        this.db = db;
        this.accounts = accounts;
        this.notificacoes = notificacoes;
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

    public record ItinerarioEstimativa(
            @NotNull @Valid Endereco origem,
            @NotNull @Valid Endereco destino,
            @NotNull @Size(max = 50) List<@NotNull @Valid Endereco> paradas) {
        public List<String> enderecos() {
            var pontos = new java.util.ArrayList<String>();
            pontos.add(enderecoViario(origem));
            paradas.forEach(parada -> pontos.add(enderecoViario(parada)));
            pontos.add(enderecoViario(destino));
            return pontos;
        }

        private static String enderecoViario(Endereco endereco) {
            return String.join(", ", endereco.logradouro().trim(), endereco.numero().trim(),
                    endereco.cidade(), endereco.estado().toUpperCase(Locale.ROOT));
        }
    }

    @Transactional
    public long enviar(String email, Envio envio, EstimativaFreteService.Estimativa estimativa) {
        long passageiro = bloquearPassageiro(email);
        Long anterior = envioAnterior(passageiro, envio.chave(), "PESSOA");
        if (anterior != null) return anterior;

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

        if (estimativa.piso() != null && envio.valor() != null && envio.valor().compareTo(estimativa.piso()) < 0) {
            String piso = java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(estimativa.piso());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Informe uma oferta de pelo menos " + piso + " ou envie sem oferta.");
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
                    valor_cliente, condicoes_cliente, chave_envio, percurso_km)
                VALUES ('PESSOA', ?, ?, 'ABERTA', ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, envio.partida(), envio.retorno(), passageiro, envio.prestadorId(),
                origem, destino, envio.valor(), condicoes, envio.chave(), estimativa.distanciaKm());
        db.update("INSERT INTO solicitacao_pessoa (id, quantidade_pessoas) VALUES (?, ?)", id, envio.passageiros());
        int ordem = 1;
        for (Endereco parada : envio.paradas()) {
            db.update("INSERT INTO solicitacao_parada (id_solicitacao, ordem, endereco_id) VALUES (?, ?, ?)",
                    id, ordem++, gravarEndereco(parada));
        }
        notificarPrestador(envio.prestadorId(), id);
        return id;
    }

    public record Contato(@NotBlank @Size(max = 100) String nome,
                          @NotBlank @Size(max = 20) String telefone,
                          @Size(max = 255) String instrucoes) {}

    public record EnvioEncomenda(
            @NotNull UUID chave, @Positive long prestadorId,
            @NotNull LocalDateTime partida,
            @NotNull @Valid Endereco origem, @NotNull @Valid Endereco destino,
            @NotBlank @Size(max = 255) String descricaoVolumes,
            @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal pesoKg,
            @NotNull @Positive @Digits(integer = 5, fraction = 2) BigDecimal alturaCm,
            @NotNull @Positive @Digits(integer = 5, fraction = 2) BigDecimal larguraCm,
            @NotNull @Positive @Digits(integer = 5, fraction = 2) BigDecimal profundidadeCm,
            @NotNull @Valid Contato coleta, @NotNull @Valid Contato entrega) {}

    @Transactional
    public long enviarEncomenda(String email, EnvioEncomenda envio) {
        long passageiro = bloquearPassageiro(email);
        Long anterior = envioAnterior(passageiro, envio.chave(), "ENCOMENDA");
        if (anterior != null) return anterior;
        if (passageiro == envio.prestadorId() || !envio.partida().isAfter(LocalDateTime.now(ZoneId.of("America/Fortaleza")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confira o destinatário e o horário de coleta.");
        }
        var elegiveis = db.query("""
                SELECT v.id FROM veiculo v
                JOIN autorizacao_modalidade a ON a.id_prestador = v.id_prestador
                WHERE v.id_prestador = ? AND v.ativo
                  AND v.modalidade IN ('ENCOMENDA', 'AMBOS') AND v.capacidade_carga_kg >= ?
                  AND a.modalidade = 'ENCOMENDA' AND a.status = 'APROVADO'
                ORDER BY v.id LIMIT 1 FOR SHARE OF v, a
                """, (rs, row) -> rs.getLong(1), envio.prestadorId(), envio.pesoKg());
        if (elegiveis.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O prestador não possui autorização e veículo ativo compatível com o peso informado.");
        }
        long origem = gravarEndereco(envio.origem());
        long destino = gravarEndereco(envio.destino());
        long id = db.queryForObject("""
                INSERT INTO solicitacao_frete (modalidade, data_hora_pretendida, status, id_passageiro,
                    id_prestador, endereco_origem_id, endereco_destino_id, chave_envio)
                VALUES ('ENCOMENDA', ?, 'ABERTA', ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, envio.partida(), passageiro, envio.prestadorId(), origem, destino, envio.chave());
        db.update("""
                INSERT INTO solicitacao_encomenda (id, descricao_volumes, peso_estimado_kg,
                    altura_cm, largura_cm, profundidade_cm, nome_contato_coleta, telefone_contato_coleta,
                    instrucoes_coleta, nome_contato_entrega, telefone_contato_entrega, instrucoes_entrega)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, envio.descricaoVolumes().strip(), envio.pesoKg(), envio.alturaCm(), envio.larguraCm(),
                envio.profundidadeCm(), envio.coleta().nome().strip(), envio.coleta().telefone().strip(),
                envio.coleta().instrucoes(), envio.entrega().nome().strip(), envio.entrega().telefone().strip(),
                envio.entrega().instrucoes());
        notificarPrestador(envio.prestadorId(), id);
        return id;
    }

    public Long consultarEnvioPessoa(String email, UUID chave) {
        long passageiro = localizarConta(email);
        if (db.query("SELECT id FROM passageiro WHERE id = ?", (rs, row) -> rs.getLong(1), passageiro).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta conta não tem perfil de passageiro.");
        }
        return chave == null ? null : envioAnterior(passageiro, chave, "PESSOA");
    }

    private long localizarConta(String email) {
        return accounts.findAccount(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)).id();
    }

    private long bloquearPassageiro(String email) {
        long passageiro = localizarConta(email);
        if (db.query("SELECT id FROM passageiro WHERE id = ? FOR UPDATE",
                (rs, row) -> rs.getLong(1), passageiro).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta conta não tem perfil de passageiro.");
        }
        return passageiro;
    }

    private Long envioAnterior(long passageiro, UUID chave, String modalidade) {
        return db.query("""
                SELECT id, modalidade FROM solicitacao_frete WHERE id_passageiro = ? AND chave_envio = ?
                """, (rs, row) -> {
                    if (!modalidade.equals(rs.getString("modalidade"))) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "A chave já foi usada em outra modalidade.");
                    }
                    return rs.getLong("id");
                }, passageiro, chave).stream().findFirst().orElse(null);
    }

    private void notificarPrestador(long prestador, long id) {
        notificacoes.notificar(prestador, "Nova solicitação direcionada",
                "Consulte o roteiro e responda à solicitação.", null, id);
    }

    private long gravarEndereco(Endereco endereco) {
        return db.queryForObject("""
                INSERT INTO endereco (cep, logradouro, numero, bairro, cidade, estado)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, endereco.cep(), endereco.logradouro().trim(), endereco.numero().trim(),
                endereco.bairro().trim(), endereco.cidade().trim(), endereco.estado().toUpperCase(Locale.ROOT));
    }
}
