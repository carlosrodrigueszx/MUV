package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.muv.backend.dto.DesignacaoFrete.ComandoDesignacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = "selenium.browser=chrome")
@ActiveProfiles("rf001-local")
@Sql(scripts = {"/db/rf009-clean.sql", "/db/rf009-designacao.sql"})
@Sql(scripts = "/db/rf009-clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DesignacaoIntegrationTest {

    @Autowired
    private DesignacaoService service;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void persisteParCompletoEstadoRevisaoEHistorico() {
        var resultado = service.designar(
                909060L,
                909002L,
                new ComandoDesignacao(909010L, 909020L, 0L));

        assertThat(resultado.status()).isEqualTo("PRONTO_INICIO");
        assertThat(resultado.revisao()).isEqualTo(1L);
        var frete = jdbc.queryForMap("""
                SELECT status, motorista_atual_id, veiculo_atual_id, revisao_designacao
                FROM frete WHERE id = 909060
                """);
        assertThat(frete)
                .containsEntry("status", "PRONTO_INICIO")
                .containsEntry("motorista_atual_id", 909010L)
                .containsEntry("veiculo_atual_id", 909020L)
                .containsEntry("revisao_designacao", 1L);
        var evento = jdbc.queryForMap("""
                SELECT tipo_evento, usuario_autor_id, motorista_id, veiculo_id
                FROM historico_execucao_frete
                WHERE frete_id = 909060
                """);
        assertThat(evento)
                .containsEntry("tipo_evento", "DESIGNACAO")
                .containsEntry("usuario_autor_id", 909002L)
                .containsEntry("motorista_id", 909010L)
                .containsEntry("veiculo_id", 909020L);
    }

    @Test
    void desfazAtualizacaoDoFreteQuandoHistoricoFalha() {
        jdbc.execute("""
                ALTER TABLE historico_execucao_frete
                ADD CONSTRAINT rf009_falha_historico
                CHECK (frete_id <> 909060 OR tipo_evento <> 'DESIGNACAO')
                """);
        try {
            assertThrows(DataAccessException.class, () -> service.designar(
                    909060L,
                    909002L,
                    new ComandoDesignacao(909010L, 909020L, 0L)));

            var frete = jdbc.queryForMap("""
                    SELECT status, motorista_atual_id, veiculo_atual_id, revisao_designacao
                    FROM frete WHERE id = 909060
                    """);
            assertThat(frete)
                    .containsEntry("status", "AGUARDANDO_DESIGNACAO")
                    .containsEntry("motorista_atual_id", null)
                    .containsEntry("veiculo_atual_id", null)
                    .containsEntry("revisao_designacao", 0L);
            assertThat(jdbc.queryForObject(
                    "SELECT count(*) FROM historico_execucao_frete WHERE frete_id = 909060",
                    Integer.class)).isZero();
        } finally {
            jdbc.execute("""
                    ALTER TABLE historico_execucao_frete
                    DROP CONSTRAINT rf009_falha_historico
                    """);
        }
    }

    @Test
    void freteSemParCompletoPermaneceAguardandoDesignacao() {
        assertThrows(ResponseStatusException.class, () -> service.designar(
                909060L,
                909002L,
                new ComandoDesignacao(null, 909020L, 0L)));
        assertThrows(ResponseStatusException.class, () -> service.designar(
                909060L,
                909002L,
                new ComandoDesignacao(909010L, null, 0L)));

        var frete = jdbc.queryForMap("""
                SELECT status, motorista_atual_id, veiculo_atual_id, revisao_designacao
                FROM frete WHERE id = 909060
                """);
        assertThat(frete)
                .containsEntry("status", "AGUARDANDO_DESIGNACAO")
                .containsEntry("motorista_atual_id", null)
                .containsEntry("veiculo_atual_id", null)
                .containsEntry("revisao_designacao", 0L);
    }
}
