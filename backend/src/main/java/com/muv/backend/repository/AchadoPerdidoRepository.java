package com.muv.backend.repository;

import com.muv.backend.dto.AchadoPerdidoResponse;
import com.muv.backend.dto.AchadoPerdidoOpcoesResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AchadoPerdidoRepository {
    private final JdbcClient db;

    public AchadoPerdidoRepository(JdbcClient db) {
        this.db = db;
    }

    public List<Long> prestadoresDoUsuario(long usuarioId) {
        return db.sql("""
                SELECT id FROM prestador WHERE id = :usuarioId
                UNION
                SELECT vm.prestador_id
                FROM motorista m
                JOIN vinculo_motorista vm ON vm.motorista_id = m.id
                WHERE m.usuario_id = :usuarioId
                ORDER BY id
                """).param("usuarioId", usuarioId).query(Long.class).list();
    }

    public Optional<Long> prestadorDoVeiculo(long veiculoId, List<Long> prestadores) {
        return db.sql("""
                SELECT id_prestador FROM veiculo
                WHERE id = :veiculoId AND id_prestador IN (:prestadores)
                """).param("veiculoId", veiculoId).param("prestadores", prestadores)
                .query(Long.class).optional();
    }

    public AchadoPerdidoOpcoesResponse opcoes(List<Long> prestadores) {
        var veiculos = db.sql("""
                SELECT id, id_prestador, marca, modelo, placa FROM veiculo
                WHERE id_prestador IN (:prestadores) AND ativo = TRUE
                ORDER BY marca, modelo, placa
                """).param("prestadores", prestadores)
                .query((rs, row) -> new AchadoPerdidoOpcoesResponse.Veiculo(
                        rs.getLong("id"), rs.getLong("id_prestador"), rs.getString("marca"),
                        rs.getString("modelo"), rs.getString("placa"))).list();
        var fretes = db.sql("""
                SELECT id, prestador_responsavel_id, status FROM frete
                WHERE prestador_responsavel_id IN (:prestadores)
                ORDER BY criado_em DESC, id DESC
                """).param("prestadores", prestadores)
                .query((rs, row) -> new AchadoPerdidoOpcoesResponse.Frete(
                        rs.getLong("id"), rs.getLong("prestador_responsavel_id"), rs.getString("status"))).list();
        return new AchadoPerdidoOpcoesResponse(veiculos, fretes);
    }

    public boolean fretePertenceAoPrestador(long freteId, long prestadorId) {
        return db.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM frete
                    WHERE id = :freteId AND prestador_responsavel_id = :prestadorId
                )
                """).param("freteId", freteId).param("prestadorId", prestadorId)
                .query(Boolean.class).single();
    }

    public long inserir(String descricao, LocalDateTime dataEncontrado, long veiculoId,
            Long freteId, long registradorId, byte[] fotoBytes, String fotoTipo) {
        long itemId;
        if (freteId == null) {
            itemId = db.sql("""
                    INSERT INTO achado_perdido
                        (descricao, data_encontrado, status, id_veiculo, id_registrador)
                    VALUES (:descricao, :dataEncontrado, 'EM_GUARDA', :veiculoId, :registradorId)
                    RETURNING id
                    """).param("descricao", descricao).param("dataEncontrado", dataEncontrado)
                    .param("veiculoId", veiculoId).param("registradorId", registradorId)
                    .query(Long.class).single();
        } else {
            itemId = db.sql("""
                    INSERT INTO achado_perdido
                        (descricao, data_encontrado, status, id_veiculo, id_registrador, id_frete)
                    VALUES (:descricao, :dataEncontrado, 'EM_GUARDA', :veiculoId, :registradorId, :freteId)
                    RETURNING id
                    """).param("descricao", descricao).param("dataEncontrado", dataEncontrado)
                    .param("veiculoId", veiculoId).param("registradorId", registradorId)
                    .param("freteId", freteId).query(Long.class).single();
        }
        db.sql("""
                INSERT INTO foto_item_perdido (conteudo, tipo, id_item)
                VALUES (:conteudo, :tipo, :itemId)
                """).param("conteudo", fotoBytes).param("tipo", fotoTipo)
                .param("itemId", itemId).update();
        return itemId;
    }

    public List<AchadoPerdidoResponse> listar(List<Long> prestadores) {
        return db.sql("""
                SELECT a.id, a.descricao, a.data_encontrado, a.status, a.id_veiculo,
                       v.marca, v.modelo, v.placa, a.id_frete,
                       foto.url AS foto_url, foto.conteudo AS foto_conteudo, foto.tipo AS foto_tipo
                FROM achado_perdido a
                JOIN veiculo v ON v.id = a.id_veiculo
                LEFT JOIN LATERAL (
                    SELECT url, conteudo, tipo FROM foto_item_perdido
                    WHERE id_item = a.id ORDER BY id LIMIT 1
                ) foto ON TRUE
                WHERE v.id_prestador IN (:prestadores)
                ORDER BY a.data_encontrado DESC, a.id DESC
                """).param("prestadores", prestadores).query((rs, row) -> {
            byte[] bytes = rs.getBytes("foto_conteudo");
            String foto = bytes == null ? rs.getString("foto_url")
                    : "data:" + rs.getString("foto_tipo") + ";base64," +
                            java.util.Base64.getEncoder().encodeToString(bytes);
            return new AchadoPerdidoResponse(rs.getLong("id"), rs.getString("descricao"),
                    rs.getObject("data_encontrado", LocalDateTime.class), rs.getString("status"),
                    rs.getLong("id_veiculo"), rs.getString("marca"), rs.getString("modelo"),
                    rs.getString("placa"), rs.getObject("id_frete", Long.class), foto);
        }).list();
    }

    public Optional<String> situacaoParaAtualizar(long id, List<Long> prestadores) {
        return db.sql("""
                SELECT a.status FROM achado_perdido a
                JOIN veiculo v ON v.id = a.id_veiculo
                WHERE a.id = :id AND v.id_prestador IN (:prestadores)
                FOR UPDATE OF a
                """).param("id", id).param("prestadores", prestadores)
                .query(String.class).optional();
    }

    public void marcarDevolvido(long id) {
        db.sql("UPDATE achado_perdido SET status = 'DEVOLVIDO' WHERE id = :id")
                .param("id", id).update();
    }
}
