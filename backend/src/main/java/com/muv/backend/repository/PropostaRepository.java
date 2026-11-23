package com.muv.backend.repository;

import com.muv.backend.dto.AvaliacaoSolicitacao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PropostaRepository {
    private final JdbcTemplate jdbc;

    public PropostaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Contexto(AvaliacaoSolicitacao detalhe, long clienteId) {}
    public record Registro(long id, BigDecimal valor, String condicoes) {}

    public Optional<Contexto> consultar(long solicitacao, long prestador, boolean bloquear) {
        String sql = """
            SELECT s.*, r.justificativa, cliente.nome, concat_ws(', ', o.logradouro, o.numero, o.cidade, o.estado) origem,
                   concat_ws(', ', d.logradouro, d.numero, d.cidade, d.estado) destino,
                   p.quantidade_pessoas, p.descricao_bagagens, e.descricao_volumes, e.peso_estimado_kg,
                   e.instrucoes_coleta, e.instrucoes_entrega
            FROM solicitacao_frete s
            JOIN usuario cliente ON cliente.id=s.id_passageiro
            JOIN endereco o ON o.id=s.endereco_origem_id
            JOIN endereco d ON d.id=s.endereco_destino_id
            JOIN autorizacao_modalidade a ON a.id_prestador=? AND a.modalidade=s.modalidade AND a.status='APROVADO'
            LEFT JOIN recusa_solicitacao r ON r.id_solicitacao=s.id AND r.id_prestador=a.id_prestador
            LEFT JOIN solicitacao_pessoa p ON p.id=s.id
            LEFT JOIN solicitacao_encomenda e ON e.id=s.id
            WHERE s.id=? AND (s.id_prestador IS NULL OR s.id_prestador=a.id_prestador)
            """;
        if (bloquear) sql += " FOR UPDATE OF s FOR SHARE OF a";
        return jdbc.query(sql, (rs, row) -> new Contexto(new AvaliacaoSolicitacao(
                rs.getLong("id"), rs.getString("modalidade"), rs.getString("status"), rs.getString("nome"),
                rs.getObject("data_hora_pretendida", LocalDateTime.class), rs.getString("origem"), rs.getString("destino"),
                rs.getObject("quantidade_pessoas", Integer.class), rs.getString("descricao_bagagens"),
                rs.getString("descricao_volumes"), rs.getBigDecimal("peso_estimado_kg"), rs.getString("instrucoes_coleta"),
                rs.getString("instrucoes_entrega"), rs.getBigDecimal("valor_cliente"), rs.getString("condicoes_cliente"), rs.getString("justificativa"), paradas(solicitacao),
                rs.getString("mapa_caminho"), rs.getBigDecimal("percurso_km"),
                rs.getObject("deslocamento_minutos", Integer.class), rs.getObject("duracao_minutos", Integer.class),
                propostaEnviada(solicitacao, prestador)),
                rs.getLong("id_passageiro")), prestador, solicitacao).stream().findFirst();
    }

    private java.util.List<AvaliacaoSolicitacao.Parada> paradas(long solicitacao) {
        return jdbc.query("""
                SELECT concat_ws(', ', e.logradouro,e.numero,e.cidade,e.estado),p.horario
                FROM solicitacao_parada p JOIN endereco e ON e.id=p.endereco_id
                WHERE p.id_solicitacao=? ORDER BY p.ordem
                """, (rs, row) -> new AvaliacaoSolicitacao.Parada(rs.getString(1),
                rs.getObject(2, LocalDateTime.class)), solicitacao);
    }

    // Consulta separada para enxergar o envio confirmado enquanto se aguardava o lock da solicitação.
    private boolean propostaEnviada(long solicitacao, long prestador) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM proposta WHERE id_solicitacao=? AND id_prestador=?)
                """, Boolean.class, solicitacao, prestador));
    }

    public void recusar(long solicitacao, long prestador, String justificativa) {
        jdbc.update("""
                INSERT INTO recusa_solicitacao(id_solicitacao,id_prestador,justificativa) VALUES (?,?,?)
                ON CONFLICT (id_solicitacao,id_prestador) DO UPDATE SET justificativa=excluded.justificativa
                """, solicitacao, prestador, justificativa);
    }

    public Optional<Registro> envioAnterior(long solicitacao, long prestador, UUID chave) {
        return jdbc.query("SELECT id, valor, condicoes FROM proposta WHERE id_solicitacao=? AND id_prestador=? AND chave_envio=?",
                (rs, row) -> new Registro(rs.getLong(1), rs.getBigDecimal(2), rs.getString(3)), solicitacao, prestador, chave)
                .stream().findFirst();
    }

    public long registrar(long solicitacao, long prestador, BigDecimal valor, String condicoes, UUID chave) {
        return jdbc.queryForObject("""
                INSERT INTO proposta(status, valor, condicoes, id_solicitacao, id_prestador, chave_envio)
                VALUES ('ENVIADA', ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, valor, condicoes, solicitacao, prestador, chave);
    }

}
