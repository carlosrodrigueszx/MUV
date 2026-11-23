package com.muv.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class NotificacaoRepository {
    private final JdbcClient db;

    public NotificacaoRepository(JdbcClient db) {
        this.db = db;
    }

    public void registrar(long destinatarioId, String titulo, String mensagem,
                          Long freteId, Long solicitacaoId) {
        db.sql("""
                INSERT INTO notificacao (titulo, mensagem, usuario_destino_id, frete_id, solicitacao_id)
                VALUES (:titulo, :mensagem, :destinatario, :frete, :solicitacao)
                """)
                .param("titulo", titulo)
                .param("mensagem", mensagem)
                .param("destinatario", destinatarioId)
                .param("frete", freteId)
                .param("solicitacao", solicitacaoId)
                .update();
    }

    public void registrar(long destinatarioId, String titulo, String mensagem,
                          Long freteId, Long solicitacaoId, LocalDateTime criadoEm) {
        db.sql("""
                INSERT INTO notificacao
                    (titulo, mensagem, usuario_destino_id, frete_id, solicitacao_id, criado_em)
                VALUES (:titulo, :mensagem, :destinatario, :frete, :solicitacao, :criadoEm)
                """)
                .param("titulo", titulo)
                .param("mensagem", mensagem)
                .param("destinatario", destinatarioId)
                .param("frete", freteId)
                .param("solicitacao", solicitacaoId)
                .param("criadoEm", criadoEm)
                .update();
    }

    public record Registro(long id, String titulo, String mensagem, LocalDateTime criadoEm,
                           boolean lida, Long freteId, Long solicitacaoId, String perfil) {}

    public List<Registro> listar(long usuarioId, boolean somenteNaoLidas) {
        return db.sql("""
                SELECT n.id, n.titulo, n.mensagem, n.criado_em, n.lida, n.frete_id, n.solicitacao_id,
                       CASE WHEN s.id_passageiro = :usuario THEN 'CLIENTE'
                            WHEN COALESCE(f.prestador_responsavel_id, s.id_prestador) = :usuario THEN 'PRESTADOR'
                            ELSE NULL END AS perfil
                FROM notificacao n
                LEFT JOIN frete f ON f.id = n.frete_id
                LEFT JOIN solicitacao_frete s ON s.id = COALESCE(n.solicitacao_id, f.solicitacao_id)
                WHERE n.usuario_destino_id = :usuario
                  AND (:somenteNaoLidas = false OR n.lida = false)
                ORDER BY n.criado_em DESC, n.id DESC
                """)
                .param("usuario", usuarioId)
                .param("somenteNaoLidas", somenteNaoLidas)
                .query((rs, row) -> new Registro(rs.getLong("id"), rs.getString("titulo"),
                        rs.getString("mensagem"), rs.getObject("criado_em", LocalDateTime.class),
                        rs.getBoolean("lida"), nullableLong(rs, "frete_id"),
                        nullableLong(rs, "solicitacao_id"), rs.getString("perfil")))
                .list();
    }

    public long contarNaoLidas(long usuarioId) {
        return db.sql("SELECT count(*) FROM notificacao WHERE usuario_destino_id = :usuario AND lida = false")
                .param("usuario", usuarioId)
                .query(Long.class)
                .single();
    }

    public void marcarComoLida(long usuarioId, long notificacaoId) {
        db.sql("UPDATE notificacao SET lida = true WHERE id = :id AND usuario_destino_id = :usuario")
                .param("id", notificacaoId)
                .param("usuario", usuarioId)
                .update();
    }

    public void marcarTodasComoLidas(long usuarioId) {
        db.sql("UPDATE notificacao SET lida = true WHERE usuario_destino_id = :usuario AND lida = false")
                .param("usuario", usuarioId)
                .update();
    }

    private static Long nullableLong(java.sql.ResultSet result, String column) throws java.sql.SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }
}
