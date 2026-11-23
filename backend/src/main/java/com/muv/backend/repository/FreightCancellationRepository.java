package com.muv.backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class FreightCancellationRepository {
    private final JdbcClient db;

    public FreightCancellationRepository(JdbcClient db) { this.db = db; }

    public record Freight(long id, long clienteId, long prestadorId, String status) {}
    public record Details(long id, long clienteId, long prestadorId, String status,
                          String clienteNome, String prestadorNome, String modalidade,
                          String origem, String destino, LocalDateTime dataHoraPretendida,
                          LocalDateTime contratadoEm) {}
    // dataHora is UTC, following the RF011 contract for the existing TIMESTAMP column.
    public record CancellationEvent(String autor, LocalDateTime dataHora, String justificativa) {}

    public Optional<Freight> findForUpdate(long id) {
        return db.sql("""
                SELECT f.id, s.id_passageiro, f.prestador_responsavel_id, f.status
                FROM frete f JOIN solicitacao_frete s ON s.id = f.solicitacao_id
                WHERE f.id = :id FOR UPDATE OF f
                """).param("id", id).query((rs, row) -> new Freight(rs.getLong("id"),
                rs.getLong("id_passageiro"), rs.getLong("prestador_responsavel_id"),
                rs.getString("status"))).optional();
    }

    public Optional<Details> findDetails(long id) {
        return db.sql("""
                SELECT f.id, s.id_passageiro, f.prestador_responsavel_id, f.status,
                       c.nome AS cliente_nome, p.nome AS prestador_nome, s.modalidade,
                       o.cidade AS origem, d.cidade AS destino, s.data_hora_pretendida, f.criado_em
                FROM frete f JOIN solicitacao_frete s ON s.id = f.solicitacao_id
                JOIN usuario c ON c.id = s.id_passageiro
                JOIN usuario p ON p.id = f.prestador_responsavel_id
                JOIN endereco o ON o.id = s.endereco_origem_id
                JOIN endereco d ON d.id = s.endereco_destino_id
                WHERE f.id = :id
                """).param("id", id).query((rs, row) -> new Details(rs.getLong("id"),
                rs.getLong("id_passageiro"), rs.getLong("prestador_responsavel_id"), rs.getString("status"),
                rs.getString("cliente_nome"), rs.getString("prestador_nome"), rs.getString("modalidade"),
                rs.getString("origem"), rs.getString("destino"),
                rs.getObject("data_hora_pretendida", LocalDateTime.class),
                rs.getObject("criado_em", LocalDateTime.class))).optional();
    }

    public Optional<CancellationEvent> findCancellation(long id) {
        return db.sql("""
                SELECT u.nome, h.data_hora, h.justificativa FROM historico_execucao_frete h
                JOIN usuario u ON u.id = h.usuario_autor_id
                WHERE h.frete_id = :id AND h.tipo_evento = 'CANCELAMENTO'
                ORDER BY h.data_hora DESC, h.id DESC LIMIT 1
                """).param("id", id).query((rs, row) -> new CancellationEvent(rs.getString("nome"),
                rs.getTimestamp("data_hora").toLocalDateTime(), rs.getString("justificativa"))).optional();
    }

    public LocalDateTime markCancelled(long id) {
        return db.sql("""
                UPDATE frete SET status = 'CANCELADO'
                WHERE id = :id AND status IN ('AGUARDANDO_DESIGNACAO', 'PRONTO_INICIO')
                RETURNING timezone('UTC', clock_timestamp())
                """).param("id", id).query(LocalDateTime.class).optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "O estado do frete mudou. Atualize a página."));
    }

    public void addCancellationEvent(long id, long author, String reason, LocalDateTime cancelledAtUtc) {
        db.sql("""
                INSERT INTO historico_execucao_frete
                    (frete_id, usuario_autor_id, tipo_evento, justificativa, data_hora)
                VALUES (:id, :author, 'CANCELAMENTO', :reason, :cancelledAtUtc)
                """).param("id", id).param("author", author).param("reason", reason)
                .param("cancelledAtUtc", cancelledAtUtc).update();
    }

}
