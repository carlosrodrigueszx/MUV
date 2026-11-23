package com.muv.backend.execucao;

import com.muv.backend.execucao.RegrasExecucao.Acao;
import com.muv.backend.execucao.RegrasExecucao.Contexto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ExecucaoFreteRepository {
    private final JdbcClient db;

    public ExecucaoFreteRepository(JdbcClient db) {
        this.db = db;
    }

    record Frete(
            long id, String estado, long revisaoDesignacao, long prestadorId, Long contaMotorista,
            Long motoristaId, String motorista, Long veiculoId, String veiculo, String placa,
            String modalidade, Integer quantidadePessoas, BigDecimal pesoEstimadoKg) {}

    Optional<Frete> consultarFrete(long id) {
        return db.sql("""
                SELECT f.id, f.status, f.revisao_designacao, f.prestador_responsavel_id,
                       m.usuario_id, m.id AS motorista_id, m.nome AS motorista, v.id AS veiculo_id,
                       concat_ws(' ', v.marca, v.modelo) AS veiculo, v.placa,
                       s.modalidade, p.quantidade_pessoas, e.peso_estimado_kg
                FROM frete f
                JOIN solicitacao_frete s ON s.id = f.solicitacao_id
                LEFT JOIN motorista m ON m.id = f.motorista_atual_id
                LEFT JOIN veiculo v ON v.id = f.veiculo_atual_id
                LEFT JOIN solicitacao_pessoa p ON p.id = s.id
                LEFT JOIN solicitacao_encomenda e ON e.id = s.id
                WHERE f.id = :id
                """)
                .param("id", id)
                .query((rs, row) -> new Frete(
                        rs.getLong("id"), rs.getString("status"), rs.getLong("revisao_designacao"),
                        rs.getLong("prestador_responsavel_id"), rs.getObject("usuario_id", Long.class),
                        rs.getObject("motorista_id", Long.class), rs.getString("motorista"),
                        rs.getObject("veiculo_id", Long.class), rs.getString("veiculo"), rs.getString("placa"),
                        rs.getString("modalidade"), rs.getObject("quantidade_pessoas", Integer.class),
                        rs.getBigDecimal("peso_estimado_kg")))
                .optional();
    }

    Optional<Frete> bloquearFrete(long id) {
        return db.sql("SELECT f.id FROM frete f WHERE f.id = :id FOR UPDATE OF f")
                .param("id", id).query(Long.class).optional()
                .flatMap(this::consultarFrete);
    }

    List<ExecucaoFreteService.Evento> consultarEventos(long freteId) {
        return db.sql("""
                SELECT h.id, h.tipo_evento, autor.nome AS autor, h.descricao, h.justificativa,
                       CASE WHEN ri.evento_id IS NOT NULL OR rr.evento_id IS NOT NULL
                              OR re.evento_id IS NOT NULL THEN h.data_hora END AS registro_utc
                FROM historico_execucao_frete h
                JOIN usuario autor ON autor.id = h.usuario_autor_id
                LEFT JOIN rf010_recibo_interrupcao ri ON ri.evento_id = h.id AND ri.frete_id = h.frete_id
                LEFT JOIN rf010_recibo_retomada rr ON rr.evento_id = h.id AND rr.frete_id = h.frete_id
                LEFT JOIN rf010_recibo_encerramento re ON re.evento_id = h.id AND re.frete_id = h.frete_id
                WHERE h.frete_id = :id
                  AND h.tipo_evento IN ('INICIO', 'OCORRENCIA', 'INTERRUPCAO', 'RETOMADA',
                                        'ENCERRAMENTO_SEM_CONCLUSAO')
                ORDER BY h.id DESC
                """)
                .param("id", freteId)
                .query((rs, row) -> {
                    var registroUtc = rs.getObject("registro_utc", LocalDateTime.class);
                    return new ExecucaoFreteService.Evento(
                            rs.getLong("id"), Acao.valueOf(rs.getString("tipo_evento")),
                            rs.getString("autor"), rs.getString("descricao"), rs.getString("justificativa"),
                            registroUtc == null ? null : registroUtc.atOffset(ZoneOffset.UTC));
                })
                .list();
    }

    Optional<Long> referenciaVigente(long freteId, Acao acao) {
        return db.sql("""
                SELECT id FROM (
                    SELECT id, tipo_evento
                    FROM historico_execucao_frete
                    WHERE frete_id = :id
                      AND tipo_evento IN ('INICIO', 'INTERRUPCAO', 'RETOMADA', 'ENCERRAMENTO_SEM_CONCLUSAO')
                    ORDER BY id DESC LIMIT 1
                ) AS transicao
                WHERE (tipo_evento IN ('INICIO', 'RETOMADA') AND :emExecucao)
                   OR (tipo_evento = 'INTERRUPCAO' AND NOT :emExecucao)
                """)
                .param("id", freteId).param("emExecucao", acao == Acao.INTERRUPCAO)
                .query(Long.class).optional();
    }

    record Recibo(Acao acao, long eventoId, long autorId, String justificativa, Long referenciaId,
                  Long revisaoDesignacao, String estadoAnterior, String estadoResultante,
                  LocalDateTime dataHoraInformada, String situacaoEncomenda) {}

    Optional<Recibo> consultarRecibo(long freteId, UUID chave) {
        return db.sql("""
                SELECT e.tipo_evento, r.evento_id, e.usuario_autor_id, e.justificativa, r.referencia_id,
                       r.revisao_designacao, r.estado_anterior, r.estado_resultante, r.data_hora_informada, r.situacao_encomenda
                FROM (
                    SELECT frete_id, evento_id, ciclo_execucao_id AS referencia_id, revisao_designacao,
                           estado_anterior, estado_resultante, data_hora_informada, CAST(NULL AS text) AS situacao_encomenda
                    FROM rf010_recibo_interrupcao WHERE frete_id = :freteId AND chave_operacao = :chave
                    UNION ALL
                    SELECT frete_id, evento_id, interrupcao_id, revisao_designacao,
                           estado_anterior, estado_resultante, data_hora_informada, CAST(NULL AS text)
                    FROM rf010_recibo_retomada WHERE frete_id = :freteId AND chave_operacao = :chave
                    UNION ALL
                    SELECT frete_id, evento_id, interrupcao_id, revisao_designacao,
                           estado_anterior, estado_resultante, data_hora_informada, situacao_encomenda
                    FROM rf010_recibo_encerramento WHERE frete_id = :freteId AND chave_operacao = :chave
                ) r
                JOIN historico_execucao_frete e ON e.id = r.evento_id AND e.frete_id = r.frete_id
                """)
                .param("freteId", freteId).param("chave", chave)
                .query((rs, row) -> new Recibo(Acao.valueOf(rs.getString("tipo_evento")), rs.getLong("evento_id"),
                        rs.getLong("usuario_autor_id"), rs.getString("justificativa"), rs.getLong("referencia_id"),
                        rs.getLong("revisao_designacao"), rs.getString("estado_anterior"),
                        rs.getString("estado_resultante"), rs.getObject("data_hora_informada", LocalDateTime.class),
                        rs.getString("situacao_encomenda")))
                .optional();
    }

    boolean atualizarEstado(Frete frete, String estadoResultante) {
        return db.sql("""
                UPDATE frete SET status = :resultante
                WHERE id = :id AND status = :anterior AND revisao_designacao = :revisao
                """).param("id", frete.id()).param("revisao", frete.revisaoDesignacao())
                .param("anterior", frete.estado()).param("resultante", estadoResultante).update() == 1;
    }

    long gravarEvento(Frete frete, long autorId, Acao acao, String justificativa) {
        return db.sql("""
                INSERT INTO historico_execucao_frete
                    (frete_id, usuario_autor_id, tipo_evento, justificativa, motorista_id, veiculo_id, data_hora)
                VALUES (:freteId, :autorId, :tipo, :justificativa, :motoristaId, :veiculoId,
                        timezone('UTC', clock_timestamp()))
                RETURNING id
                """)
                .param("freteId", frete.id()).param("autorId", autorId).param("tipo", acao.name())
                .param("justificativa", justificativa)
                .param("motoristaId", frete.motoristaId()).param("veiculoId", frete.veiculoId())
                .query(Long.class).single();
    }

    void gravarRecibo(long freteId, Acao acao, UUID chave, long eventoId, Contexto contexto,
                      long referencia, LocalDateTime dataHoraInformada, String situacaoEncomenda) {
        String tabela = switch (acao) {
            case INTERRUPCAO -> "rf010_recibo_interrupcao";
            case RETOMADA -> "rf010_recibo_retomada";
            case ENCERRAMENTO_SEM_CONCLUSAO -> "rf010_recibo_encerramento";
            default -> throw new IllegalArgumentException("Ação sem recibo disponível.");
        };
        String colunaReferencia = acao == Acao.INTERRUPCAO ? "ciclo_execucao_id" : "interrupcao_id";
        boolean encerramento = acao == Acao.ENCERRAMENTO_SEM_CONCLUSAO;
        var insert = db.sql("""
                INSERT INTO %s
                    (frete_id, chave_operacao, evento_id, %s, revisao_designacao,
                     estado_anterior, estado_resultante, data_hora_informada%s)
                VALUES (:freteId, :chave, :eventoId, :referencia, :revisao, :anterior, :resultante, :dataHora%s)
                """.formatted(tabela, colunaReferencia, encerramento ? ", situacao_encomenda" : "",
                              encerramento ? ", :situacao" : ""))
                .param("freteId", freteId).param("chave", chave).param("eventoId", eventoId)
                .param("referencia", referencia).param("revisao", contexto.revisaoDesignacao())
                .param("anterior", contexto.estado()).param("resultante", acao.estadoResultante())
                .param("dataHora", dataHoraInformada);
        if (encerramento) insert = insert.param("situacao", situacaoEncomenda, java.sql.Types.VARCHAR);
        insert.update();
    }
}
