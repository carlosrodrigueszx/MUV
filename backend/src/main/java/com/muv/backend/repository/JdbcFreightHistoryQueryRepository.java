package com.muv.backend.repository;

import com.muv.backend.dto.FreightHistoryProfile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.YearMonth;
import java.util.List;

@Repository
public class JdbcFreightHistoryQueryRepository implements FreightHistoryQueryRepository {

    private static final String HISTORY_SQL = """
            SELECT
                s.id AS solicitacao_id,
                f.id AS frete_id,
                s.modalidade,
                origem.cidade AS origem,
                destino.cidade AS destino,
                COALESCE(f.data_hora_conclusao, f.criado_em, s.data_hora_pretendida) AS data_referencia,
                s.criado_em AS data_abertura,
                cliente.nome AS solicitante,
                CASE
                    WHEN se.id IS NOT NULL THEN se.descricao_volumes
                    WHEN sp.id IS NOT NULL THEN CONCAT(sp.quantidade_pessoas, ' passageiro(s)')
                    ELSE 'Detalhes não informados'
                END AS resumo_solicitacao,
                f.valor_atual,
                f.condicoes_atuais,
                COALESCE(f.status, s.status) AS situacao
            FROM solicitacao_frete s
            JOIN usuario cliente ON cliente.id = s.id_passageiro
            JOIN endereco origem ON origem.id = s.endereco_origem_id
            JOIN endereco destino ON destino.id = s.endereco_destino_id
            LEFT JOIN solicitacao_encomenda se ON se.id = s.id
            LEFT JOIN solicitacao_pessoa sp ON sp.id = s.id
            LEFT JOIN frete f ON f.solicitacao_id = s.id
            WHERE
                ((:isClient = TRUE AND s.id_passageiro = :userId)
                 OR
                 (:isProvider = TRUE AND (
                     s.id_prestador = :userId
                     OR f.prestador_responsavel_id = :userId
                     OR EXISTS (
                         SELECT 1 FROM proposta access_proposal
                         WHERE access_proposal.id_solicitacao = s.id
                           AND access_proposal.id_prestador = :userId
                     )
                 )))
                AND (:modality IS NULL OR s.modalidade = :modality)
                AND (
                    :status IS NULL
                    OR (:status = 'CONTRATADO' AND f.status IN
                        ('AGUARDANDO_DESIGNACAO', 'PRONTO_INICIO', 'EM_EXECUCAO', 'INTERROMPIDO'))
                    OR (:status = 'PROPOSTA_ENVIADA' AND f.id IS NULL AND EXISTS (
                        SELECT 1 FROM proposta status_proposal
                        WHERE status_proposal.id_solicitacao = s.id
                          AND (:isClient = TRUE OR status_proposal.id_prestador = :userId)
                    ))
                    OR COALESCE(f.status, s.status) = :status
                )
                AND (:monthStart IS NULL OR
                     COALESCE(f.data_hora_conclusao, f.criado_em, s.data_hora_pretendida) >= :monthStart
                     AND COALESCE(f.data_hora_conclusao, f.criado_em, s.data_hora_pretendida) < :monthEnd)
            ORDER BY data_referencia DESC, s.id DESC
            """;

    private static final String PROPOSALS_SQL = """
            SELECT p.id, provider.nome AS prestador, p.valor, p.status
            FROM proposta p
            JOIN usuario provider ON provider.id = p.id_prestador
            WHERE p.id_solicitacao = :solicitationId
              AND (:isClient = TRUE OR p.id_prestador = :userId)
            ORDER BY p.criado_em DESC, p.id DESC
            """;

    private static final String TIMELINE_SQL = """
            SELECT h.data_hora, h.tipo_evento, author.nome AS autor,
                   h.descricao, h.justificativa
            FROM historico_execucao_frete h
            JOIN usuario author ON author.id = h.usuario_autor_id
            WHERE h.frete_id = :freightId
            ORDER BY h.data_hora DESC, h.id DESC
            """;

    private final JdbcClient jdbcClient;

    public JdbcFreightHistoryQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<HistoryRow> findHistory(
            Long userId,
            FreightHistoryProfile profile,
            String modality,
            String status,
            YearMonth month) {
        var monthStart = month == null ? null : month.atDay(1).atStartOfDay();
        var monthEnd = month == null ? null : month.plusMonths(1).atDay(1).atStartOfDay();

        return jdbcClient.sql(HISTORY_SQL)
                .param("userId", userId)
                .param("isClient", profile == FreightHistoryProfile.CLIENTE)
                .param("isProvider", profile == FreightHistoryProfile.PRESTADOR)
                .param("modality", modality, Types.VARCHAR)
                .param("status", status, Types.VARCHAR)
                .param("monthStart", monthStart, Types.TIMESTAMP)
                .param("monthEnd", monthEnd, Types.TIMESTAMP)
                .query((resultSet, rowNum) -> new HistoryRow(
                        resultSet.getLong("solicitacao_id"),
                        resultSet.getObject("frete_id", Long.class),
                        resultSet.getString("modalidade"),
                        resultSet.getString("origem"),
                        resultSet.getString("destino"),
                        resultSet.getTimestamp("data_referencia").toLocalDateTime(),
                        resultSet.getTimestamp("data_abertura").toLocalDateTime(),
                        resultSet.getString("solicitante"),
                        resultSet.getString("resumo_solicitacao"),
                        resultSet.getBigDecimal("valor_atual"),
                        resultSet.getString("condicoes_atuais"),
                        resultSet.getString("situacao")))
                .list();
    }

    @Override
    public List<ProposalRow> findProposals(
            Long solicitationId,
            Long userId,
            FreightHistoryProfile profile) {
        return jdbcClient.sql(PROPOSALS_SQL)
                .param("solicitationId", solicitationId)
                .param("userId", userId)
                .param("isClient", profile == FreightHistoryProfile.CLIENTE)
                .query((resultSet, rowNum) -> new ProposalRow(
                        resultSet.getLong("id"),
                        resultSet.getString("prestador"),
                        resultSet.getBigDecimal("valor"),
                        resultSet.getString("status")))
                .list();
    }

    @Override
    public List<TimelineRow> findTimeline(Long freightId) {
        if (freightId == null) return List.of();

        return jdbcClient.sql(TIMELINE_SQL)
                .param("freightId", freightId)
                .query((resultSet, rowNum) -> new TimelineRow(
                        resultSet.getTimestamp("data_hora").toLocalDateTime(),
                        resultSet.getString("tipo_evento"),
                        resultSet.getString("autor"),
                        resultSet.getString("descricao"),
                        resultSet.getString("justificativa")))
                .list();
    }
}
