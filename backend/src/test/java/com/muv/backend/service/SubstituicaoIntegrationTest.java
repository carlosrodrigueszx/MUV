package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.muv.backend.dto.DesignacaoFrete.ComandoSubstituicao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(properties = "selenium.browser=chrome")
@ActiveProfiles("rf001-local")
@Sql(scripts = {"/db/rf009-clean.sql", "/db/rf009-designacao.sql"})
@Sql(scripts = "/db/rf009-clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SubstituicaoIntegrationTest {

    @Autowired
    private DesignacaoService service;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registraParResultanteHistoricoENotificacaoDoCliente() {
        var resultado = service.substituir(909061L, 909002L,
                new ComandoSubstituicao(
                        null, 909021L, "Manutenção preventiva", 1L, null, null));

        assertThat(resultado.status()).isEqualTo("PRONTO_INICIO");
        assertThat(resultado.revisao()).isEqualTo(2L);
        assertThat(jdbc.queryForMap("""
                SELECT status, motorista_atual_id, veiculo_atual_id, revisao_designacao
                FROM frete WHERE id = 909061
                """))
                .containsEntry("status", "PRONTO_INICIO")
                .containsEntry("motorista_atual_id", 909010L)
                .containsEntry("veiculo_atual_id", 909021L)
                .containsEntry("revisao_designacao", 2L);
        assertThat(jdbc.queryForMap("""
                SELECT tipo_evento, usuario_autor_id, justificativa,
                       motorista_id, veiculo_id, data_hora
                FROM historico_execucao_frete
                WHERE frete_id = 909061 AND tipo_evento = 'SUBSTITUICAO_SEM_ALTERACAO'
                """))
                .containsEntry("tipo_evento", "SUBSTITUICAO_SEM_ALTERACAO")
                .containsEntry("usuario_autor_id", 909002L)
                .containsEntry("justificativa", "Manutenção preventiva")
                .containsEntry("motorista_id", 909010L)
                .containsEntry("veiculo_id", 909021L)
                .doesNotContainEntry("data_hora", null);
        assertThat(jdbc.queryForMap("""
                SELECT usuario_destino_id, frete_id, titulo
                FROM notificacao WHERE frete_id = 909061
                """))
                .containsEntry("usuario_destino_id", 909001L)
                .containsEntry("frete_id", 909061L)
                .containsEntry("titulo", "Recursos do frete atualizados");
    }

    @Test
    void falhaNaNotificacaoDesfazFreteEHistorico() {
        jdbc.execute("""
                ALTER TABLE notificacao
                ADD CONSTRAINT rf009_falha_notificacao CHECK (frete_id <> 909061)
                """);
        try {
            assertThrows(DataAccessException.class, () -> service.substituir(909061L, 909002L,
                    new ComandoSubstituicao(
                            null, 909021L, "Manutenção preventiva", 1L, null, null)));

            assertThat(jdbc.queryForMap("""
                    SELECT status, motorista_atual_id, veiculo_atual_id, revisao_designacao
                    FROM frete WHERE id = 909061
                    """))
                    .containsEntry("status", "PRONTO_INICIO")
                    .containsEntry("motorista_atual_id", 909010L)
                    .containsEntry("veiculo_atual_id", 909024L)
                    .containsEntry("revisao_designacao", 1L);
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM historico_execucao_frete
                    WHERE frete_id = 909061 AND tipo_evento = 'SUBSTITUICAO_SEM_ALTERACAO'
                    """, Integer.class)).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM notificacao WHERE frete_id = 909061",
                    Integer.class)).isZero();
        } finally {
            jdbc.execute("""
                    ALTER TABLE notificacao DROP CONSTRAINT rf009_falha_notificacao
                    """);
        }
    }

    @Test
    void alteracaoDeValorCriaAcordoSemMudarDesignacaoVigente() {
        var resultado = service.substituir(909061L, 909002L,
                new ComandoSubstituicao(
                        909018L, 909021L, "Nova composição", 1L,
                        new java.math.BigDecimal("800.00"), "Nova condição de embarque"));

        assertThat(resultado.solicitacaoAcordoId()).isNotNull();
        assertThat(resultado.revisao()).isEqualTo(1L);
        assertThat(jdbc.queryForMap("""
                SELECT valor_atual, condicoes_atuais, motorista_atual_id,
                       veiculo_atual_id, revisao_designacao
                FROM frete WHERE id = 909061
                """))
                .containsEntry("valor_atual", new java.math.BigDecimal("740.00"))
                .containsEntry("condicoes_atuais", "Três passageiros")
                .containsEntry("motorista_atual_id", 909010L)
                .containsEntry("veiculo_atual_id", 909024L)
                .containsEntry("revisao_designacao", 1L);
        assertThat(jdbc.queryForMap("""
                SELECT status, novo_valor, novas_condicoes, novo_motorista_id,
                       novo_veiculo_id, revisao_origem
                FROM solicitacao_alteracao_acordo WHERE id = ?
                """, resultado.solicitacaoAcordoId()))
                .containsEntry("status", "PENDENTE")
                .containsEntry("novo_valor", new java.math.BigDecimal("800.00"))
                .containsEntry("novas_condicoes", "Nova condição de embarque")
                .containsEntry("novo_motorista_id", 909018L)
                .containsEntry("novo_veiculo_id", 909021L)
                .containsEntry("revisao_origem", 1L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notificacao
                WHERE frete_id = 909061 AND usuario_destino_id = 909001
                  AND titulo = 'Nova proposta de alteração do frete'
                """, Integer.class)).isEqualTo(1);
    }
}
