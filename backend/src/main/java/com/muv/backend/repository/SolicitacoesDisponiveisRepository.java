package com.muv.backend.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SolicitacoesDisponiveisRepository {
    private final JdbcTemplate jdbc;

    public SolicitacoesDisponiveisRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Item(long id, String modalidade, LocalDateTime partida,
            String origemCidade, String origemEstado, String destinoCidade, String destinoEstado,
            Integer passageiros, BigDecimal pesoKg, String volumes, BigDecimal volumeM3, int paradas,
            boolean direcionada, boolean autorizada) {}

    public record Perfil(String nome, String tipo, List<String> modalidades, List<String> regioes) {}

    public java.util.Optional<Perfil> perfil(long prestador) {
        return jdbc.query("""
                SELECT COALESCE(NULLIF(e.nome_fantasia, ''), u.nome) AS nome,
                       CASE WHEN e.id IS NULL THEN 'Autônomo' ELSE 'Empresa' END AS tipo,
                       ARRAY(SELECT DISTINCT a.modalidade::text FROM autorizacao_modalidade a
                             WHERE a.id_prestador=p.id AND a.status='APROVADO' ORDER BY 1) AS modalidades,
                       ARRAY(SELECT DISTINCT concat_ws('/', NULLIF(trim(r.cidade), ''), NULLIF(trim(r.estado), ''))
                             FROM regiao_atuacao r WHERE r.id_prestador=p.id ORDER BY 1) AS regioes
                FROM prestador p JOIN usuario u ON u.id=p.id
                LEFT JOIN prestador_empresa e ON e.id=p.id WHERE p.id=?
                """, (rs, row) -> new Perfil(rs.getString("nome"), rs.getString("tipo"),
                        List.of((String[]) rs.getArray("modalidades").getArray()),
                        List.of((String[]) rs.getArray("regioes").getArray())), prestador).stream().findFirst();
    }

    public boolean existePrestador(long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM prestador WHERE id=?)", Boolean.class, id));
    }

    public List<Item> listar(long prestador, String modalidade, String cidade, String estado) {
        return listar(prestador, modalidade, cidade, estado, null);
    }

    public List<Item> listar(long prestador, String modalidade, String cidade, String estado, LocalDate data) {
        return jdbc.query("""
                SELECT s.id, s.modalidade, s.data_hora_pretendida, o.cidade origem_cidade,
                       o.estado origem_estado, d.cidade destino_cidade, d.estado destino_estado,
                       p.quantidade_pessoas, e.peso_estimado_kg, e.descricao_volumes,
                       e.altura_cm * e.largura_cm * e.profundidade_cm / 1000000 AS volume_m3,
                       (SELECT count(*) FROM solicitacao_parada sp WHERE sp.id_solicitacao=s.id) paradas,
                       s.id_prestador IS NOT NULL direcionada,
                       EXISTS (SELECT 1 FROM autorizacao_modalidade a WHERE a.id_prestador=?
                           AND a.modalidade=s.modalidade AND a.status='APROVADO') autorizada
                FROM solicitacao_frete s
                JOIN endereco o ON o.id=s.endereco_origem_id
                JOIN endereco d ON d.id=s.endereco_destino_id
                LEFT JOIN solicitacao_pessoa p ON p.id=s.id
                LEFT JOIN solicitacao_encomenda e ON e.id=s.id
                WHERE s.status='ABERTA' AND (s.id_prestador IS NULL OR s.id_prestador=?)
                  AND (?='TODAS' OR s.modalidade=?)
                  AND (CAST(? AS text) IS NULL OR lower(o.cidade)=lower(?))
                  AND (CAST(? AS text) IS NULL OR upper(o.estado)=upper(?))
                  AND (CAST(? AS date) IS NULL OR CAST(s.data_hora_pretendida AS date)=?)
                ORDER BY s.data_hora_pretendida, s.id
                """, (rs, row) -> new Item(rs.getLong("id"), rs.getString("modalidade"),
                        rs.getObject("data_hora_pretendida", LocalDateTime.class),
                        rs.getString("origem_cidade"), rs.getString("origem_estado"),
                        rs.getString("destino_cidade"), rs.getString("destino_estado"),
                        rs.getObject("quantidade_pessoas", Integer.class), rs.getBigDecimal("peso_estimado_kg"),
                        rs.getString("descricao_volumes"), rs.getBigDecimal("volume_m3"), rs.getInt("paradas"),
                        rs.getBoolean("direcionada"), rs.getBoolean("autorizada")),
                prestador, prestador, modalidade, modalidade,
                cidade == null || cidade.isBlank() ? null : cidade.trim(), cidade == null ? null : cidade.trim(),
                estado == null || estado.isBlank() ? null : estado.trim(), estado == null ? null : estado.trim(),
                data, data);
    }
}
