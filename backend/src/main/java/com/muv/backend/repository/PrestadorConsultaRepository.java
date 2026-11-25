package com.muv.backend.repository;

import com.muv.backend.dto.PrestadorConsultaResponse;
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
        StringBuilder sql = new StringBuilder("""
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
                WHERE (pe.id IS NOT NULL OR pa.id IS NOT NULL)
                """);

        if (quantidadePassageiros != null) {
            sql.append(" AND am.modalidade = :modalidade AND v.capacidade_passageiros >= :quantidadePassageiros");
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
        sql.append(" ORDER BY p.id, am.modalidade, r.estado, r.cidade, v.id");

        JdbcClient.StatementSpec query = db.sql(ModalidadesDemonstracao.aplicar(sql.toString(), dispensarVerificacao));
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
                rs.getBigDecimal("capacidade_carga_kg"))).list().forEach(row -> {
            ProviderAccumulator provider = providers.computeIfAbsent(row.id(),
                    ignored -> new ProviderAccumulator(row.id(), row.nome(), row.tipo()));
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
            java.math.BigDecimal capacidadeCargaKg) {}

    private static final class ProviderAccumulator {
        private final Long id;
        private final String nome;
        private final String tipo;
        private final Set<String> modalidades = new LinkedHashSet<>();
        private final Set<Regiao> regioes = new LinkedHashSet<>();
        private final Set<Veiculo> veiculos = new LinkedHashSet<>();

        private ProviderAccumulator(Long id, String nome, String tipo) {
            this.id = id;
            this.nome = nome;
            this.tipo = tipo;
        }

        private PrestadorConsultaResponse toResponse() {
            return new PrestadorConsultaResponse(id, nome, tipo,
                    new ArrayList<>(modalidades), new ArrayList<>(regioes), new ArrayList<>(veiculos));
        }
    }
}
