package com.muv.backend.repository;

import com.muv.backend.dto.PrestadorConsultaResponse;
import com.muv.backend.dto.PrestadorConsultaResponse.Cobertura;
import com.muv.backend.dto.PrestadorConsultaResponse.Municipio;
import com.muv.backend.dto.PrestadorConsultaResponse.Regiao;
import com.muv.backend.dto.PrestadorConsultaResponse.Veiculo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class PrestadorConsultaRepository {
    @Value("${muv.demo.dispensar-verificacao:false}")
    private boolean dispensarVerificacao;

    private final JdbcClient db;

    public PrestadorConsultaRepository(JdbcClient db) {
        this.db = db;
    }

    public List<PrestadorConsultaResponse> buscar(String modalidade, String cidade, String estado, String tipo) {
        return buscar(modalidade, cidade, estado, tipo, null);
    }

    public List<PrestadorConsultaResponse> buscar(
            String modalidade, String cidade, String estado, String tipo, Integer quantidadePassageiros) {
        return consultar(modalidade, cidade, estado, tipo, quantidadePassageiros, null, 0);
    }

    public List<PrestadorConsultaResponse> buscarRegional(String modalidade, String tipo,
            Integer quantidadePassageiros, int municipioCodigo, double raioKm) {
        return consultar(modalidade, null, null, tipo, quantidadePassageiros, municipioCodigo, raioKm);
    }

    public boolean existeMunicipio(int codigo) {
        return db.sql("SELECT EXISTS (SELECT 1 FROM municipio WHERE codigo_ibge = :codigo)")
                .param("codigo", codigo).query(Boolean.class).single();
    }

    public List<Municipio> listarMunicipios() {
        return db.sql("SELECT codigo_ibge, nome, uf FROM municipio ORDER BY uf, nome, codigo_ibge")
                .query((rs, row) -> new Municipio(rs.getInt(1), rs.getString(2), rs.getString(3))).list();
    }

    private List<PrestadorConsultaResponse> consultar(String modalidade, String cidade, String estado,
            String tipo, Integer quantidadePassageiros, Integer municipioCodigo, double raioKm) {
        boolean regional = municipioCodigo != null;
        StringBuilder sql = new StringBuilder();
        if (regional) {
            sql.append("""
                    WITH distancias AS MATERIALIZED (
                        SELECT m.codigo_ibge, m.nome, m.uf, m.codigo_ibge = ref.codigo_ibge AS direta,
                               6371.0088 * acos(least(1.0, greatest(-1.0,
                                   sin(radians(ref.latitude)) * sin(radians(m.latitude)) +
                                   cos(radians(ref.latitude)) * cos(radians(m.latitude)) *
                                   cos(radians(m.longitude - ref.longitude))))) AS distancia
                        FROM municipio m
                        CROSS JOIN municipio ref
                        WHERE ref.codigo_ibge = :municipioCodigo
                    ),
                    cobertura AS (
                        SELECT DISTINCT ON (r.id_prestador) r.id_prestador, d.*
                        FROM distancias d
                        JOIN regiao_atuacao r ON r.municipio_codigo = d.codigo_ibge
                        WHERE d.direta OR d.distancia <= :raioKm
                        ORDER BY r.id_prestador, d.direta DESC, d.distancia, d.codigo_ibge
                    )
                    """);
        }
        sql.append("""
                SELECT p.id,
                       COALESCE(NULLIF(pe.nome_fantasia, ''), u.nome) AS nome,
                       CASE WHEN pe.id IS NOT NULL THEN 'EMPRESA' ELSE 'AUTONOMO' END AS tipo,
                       am.modalidade AS modalidade_autorizada,
                       r.cidade,
                       r.estado,
                       v.id AS veiculo_id,
                       v.marca,
                       v.modelo,
                       v.ano,
                       v.modalidade AS modalidade_veiculo,
                       v.capacidade_passageiros,
                       v.capacidade_carga_kg
                """);
        if (regional) {
            sql.append(", c.codigo_ibge, c.nome AS municipio, c.uf, c.direta");
        }
        sql.append("""

                FROM prestador p
                JOIN usuario u ON u.id = p.id
                LEFT JOIN prestador_empresa pe ON pe.id = p.id
                LEFT JOIN prestador_autonomo pa ON pa.id = p.id
                JOIN autorizacao_modalidade am
                  ON am.id_prestador = p.id AND am.status = 'APROVADO'
                JOIN veiculo v
                  ON v.id_prestador = p.id
                 AND v.ativo = TRUE
                 AND (v.modalidade = 'AMBOS' OR v.modalidade = am.modalidade)
                LEFT JOIN regiao_atuacao r ON r.id_prestador = p.id
                """);
        if (regional) {
            sql.append(" JOIN cobertura c ON c.id_prestador = p.id");
        }
        sql.append("""

                WHERE (pe.id IS NOT NULL OR pa.id IS NOT NULL)
                """);

        if (quantidadePassageiros != null) {
            sql.append(" AND am.modalidade = :modalidade AND v.capacidade_passageiros >= :quantidadePassageiros");
        } else if (regional) {
            sql.append(" AND am.modalidade = :modalidade");
        } else if (modalidade != null) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1
                         FROM autorizacao_modalidade amf
                         JOIN veiculo vf
                           ON vf.id_prestador = amf.id_prestador
                          AND vf.ativo = TRUE
                          AND (vf.modalidade = 'AMBOS' OR vf.modalidade = amf.modalidade)
                         WHERE amf.id_prestador = p.id
                           AND amf.modalidade = :modalidade
                           AND amf.status = 'APROVADO'
                     )
                    """);
        }
        if (cidade != null || estado != null) {
            sql.append("""
                     AND EXISTS (
                         SELECT 1 FROM regiao_atuacao rf
                         WHERE rf.id_prestador = p.id
                    """);
            if (cidade != null) sql.append(" AND rf.cidade ILIKE :cidade");
            if (estado != null) sql.append(" AND upper(rf.estado) = :estado");
            sql.append(")");
        }
        if (tipo != null) {
            sql.append(" AND CASE WHEN pe.id IS NOT NULL THEN 'EMPRESA' ELSE 'AUTONOMO' END = :tipo");
        }
        sql.append(" ORDER BY ");
        if (regional) {
            sql.append("c.direta DESC, CASE WHEN c.direta THEN 0 ELSE c.distancia END, ");
        }
        sql.append("p.id, am.modalidade, r.estado, r.cidade, v.id");

        JdbcClient.StatementSpec query = db.sql(
                ModalidadesDemonstracao.aplicar(sql.toString(), dispensarVerificacao && !regional));
        if (regional) {
            query.param("municipioCodigo", municipioCodigo).param("raioKm", raioKm);
        }
        if (modalidade != null) query.param("modalidade", modalidade);
        if (quantidadePassageiros != null) query.param("quantidadePassageiros", quantidadePassageiros);
        if (cidade != null) query.param("cidade", "%" + cidade + "%");
        if (estado != null) query.param("estado", estado);
        if (tipo != null) query.param("tipo", tipo);

        Map<Long, ProviderAccumulator> providers = new LinkedHashMap<>();
        query.query((rs, row) -> new ProviderRow(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("tipo"),
                rs.getString("modalidade_autorizada"),
                rs.getString("cidade"),
                rs.getString("estado"),
                rs.getLong("veiculo_id"),
                rs.getString("marca"),
                rs.getString("modelo"),
                rs.getInt("ano"),
                rs.getString("modalidade_veiculo"),
                rs.getInt("capacidade_passageiros"),
                rs.getBigDecimal("capacidade_carga_kg"),
                regional ? new Cobertura(rs.getBoolean("direta") ? "DIRETA" : "PROXIMA",
                        new Municipio(rs.getInt("codigo_ibge"), rs.getString("municipio"), rs.getString("uf"))) : null
                )).list().forEach(row -> {
            ProviderAccumulator provider = providers.computeIfAbsent(row.id(),
                    ignored -> new ProviderAccumulator(row.id(), row.nome(), row.tipo(), row.cobertura()));
            provider.modalidades.add(row.modalidadeAutorizada());
            if (row.cidade() != null) provider.regioes.add(new Regiao(row.cidade(), row.estado()));
            provider.veiculos.add(new Veiculo(row.veiculoId(), row.marca(), row.modelo(), row.ano(),
                    row.modalidadeVeiculo(), row.capacidadePassageiros(), row.capacidadeCargaKg()));
        });

        return providers.values().stream().map(ProviderAccumulator::toResponse).toList();
    }

    private record ProviderRow(
            Long id,
            String nome,
            String tipo,
            String modalidadeAutorizada,
            String cidade,
            String estado,
            Long veiculoId,
            String marca,
            String modelo,
            int ano,
            String modalidadeVeiculo,
            int capacidadePassageiros,
            java.math.BigDecimal capacidadeCargaKg,
            Cobertura cobertura) {}

    private static final class ProviderAccumulator {
        private final Long id;
        private final String nome;
        private final String tipo;
        private final Cobertura cobertura;
        private final Set<String> modalidades = new LinkedHashSet<>();
        private final Set<Regiao> regioes = new LinkedHashSet<>();
        private final Set<Veiculo> veiculos = new LinkedHashSet<>();

        private ProviderAccumulator(Long id, String nome, String tipo, Cobertura cobertura) {
            this.id = id;
            this.nome = nome;
            this.tipo = tipo;
            this.cobertura = cobertura;
        }

        private PrestadorConsultaResponse toResponse() {
            return new PrestadorConsultaResponse(id, nome, tipo,
                    new ArrayList<>(modalidades), new ArrayList<>(regioes), new ArrayList<>(veiculos), cobertura);
        }
    }
}
