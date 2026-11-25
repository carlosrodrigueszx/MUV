package com.muv.backend.repository;

import com.muv.backend.dto.Contratacao.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class ContratacaoRepository {
    @Value("${muv.demo.dispensar-verificacao:false}")
    private boolean dispensarVerificacao;

    private final JdbcTemplate jdbc;

    public ContratacaoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Solicitacao> solicitacao(long id, long cliente, boolean bloquear) {
        String sql = """
                SELECT s.id, s.modalidade, s.status, s.data_hora_pretendida,
                       concat_ws(' - ', o.cidade, o.estado) origem,
                       concat_ws(' - ', d.cidade, d.estado) destino
                FROM solicitacao_frete s
                JOIN endereco o ON o.id = s.endereco_origem_id
                JOIN endereco d ON d.id = s.endereco_destino_id
                WHERE s.id = ? AND s.id_passageiro = ?
                """;
        if (bloquear) sql += " FOR UPDATE OF s";
        return jdbc.query(sql, (rs, row) -> new Solicitacao(rs.getLong("id"),
                rs.getString("modalidade"), rs.getString("status"), rs.getString("origem"),
                rs.getString("destino"), rs.getObject("data_hora_pretendida", LocalDateTime.class)),
                id, cliente).stream().findFirst();
    }

    public List<Proposta> propostas(long solicitacao) {
        return jdbc.query(ModalidadesDemonstracao.aplicar("""
                SELECT p.id, p.id_prestador, u.nome, CASE WHEN pe.id IS NOT NULL THEN 'EMPRESA' WHEN pa.id IS NOT NULL THEN 'AUTONOMO' END tipo, p.valor, p.condicoes, p.status,
                       CASE WHEN pr.total_avaliacoes > 0 THEN pr.avaliacao_media END avaliacao,
                       EXISTS (SELECT 1 FROM autorizacao_modalidade a
                               WHERE a.id_prestador = p.id_prestador AND a.modalidade = s.modalidade
                                 AND a.status = 'APROVADO') autorizado
                FROM proposta p
                JOIN solicitacao_frete s ON s.id = p.id_solicitacao
                JOIN prestador pr ON pr.id = p.id_prestador
                LEFT JOIN prestador_empresa pe ON pe.id = pr.id
                LEFT JOIN prestador_autonomo pa ON pa.id = pr.id
                JOIN usuario u ON u.id = pr.id
                WHERE p.id_solicitacao = ?
                ORDER BY p.criado_em, p.id
                """, dispensarVerificacao), (rs, row) -> new Proposta(rs.getLong("id"), rs.getLong("id_prestador"),
                rs.getString("nome"), rs.getString("tipo"), rs.getBigDecimal("avaliacao"),
                rs.getBoolean("autorizado"), rs.getBigDecimal("valor"), rs.getString("condicoes"),
                rs.getString("status")), solicitacao);
    }

    public Optional<Frete> freteDaSolicitacao(long solicitacao) {
        return jdbc.query("""
                SELECT f.*, u.nome
                FROM frete f JOIN usuario u ON u.id = f.prestador_responsavel_id
                WHERE f.solicitacao_id = ?
                """, (rs, row) -> new Frete(rs.getLong("id"), rs.getLong("proposta_aceita_id"),
                rs.getString("nome"), rs.getBigDecimal("valor_atual"), rs.getString("condicoes_atuais"),
                rs.getString("status"), rs.getObject("criado_em", LocalDateTime.class)),
                solicitacao).stream().findFirst();
    }

    public Optional<Long> solicitacaoDoFrete(long frete, long cliente) {
        return jdbc.query("""
                SELECT s.id FROM frete f
                JOIN solicitacao_frete s ON s.id = f.solicitacao_id
                WHERE f.id = ? AND s.id_passageiro = ?
                """, (rs, row) -> rs.getLong(1), frete, cliente).stream().findFirst();
    }

    public long registrar(long solicitacao, Proposta proposta) {
        jdbc.update("UPDATE proposta SET status = 'ACEITA' WHERE id = ?", proposta.id());
        jdbc.update("UPDATE solicitacao_frete SET status = 'CONTRATADA' WHERE id = ?", solicitacao);
        return jdbc.queryForObject("""
                INSERT INTO frete (solicitacao_id, proposta_aceita_id, prestador_responsavel_id,
                                   valor_atual, condicoes_atuais, status)
                VALUES (?, ?, ?, ?, ?, 'AGUARDANDO_DESIGNACAO') RETURNING id
                """, Long.class, solicitacao, proposta.id(), proposta.prestadorId(),
                proposta.valor(), proposta.condicoes());
    }

    public void notificar(long prestador, long solicitacao, long frete) {
        jdbc.update("""
                INSERT INTO notificacao (titulo, mensagem, usuario_destino_id, solicitacao_id, frete_id)
                VALUES ('Contratação confirmada', 'O cliente aceitou sua proposta de frete.', ?, ?, ?)
                """, prestador, solicitacao, frete);
    }
}
