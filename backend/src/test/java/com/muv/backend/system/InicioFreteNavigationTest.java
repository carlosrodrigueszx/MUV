package com.muv.backend.system;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

// Usa o WebDriver do projeto e o proxy /api do Vite para localhost:8080.
@EnabledIfSystemProperty(named = "muv.rf010.e2e", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "selenium.headless=true")
@ActiveProfiles("rf010-test")
@Sql(scripts = "/db/rf010-consulta.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(statements = "ALTER SEQUENCE historico_execucao_frete_id_seq RESTART WITH 910075",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class InicioFreteNavigationTest extends NavegacaoExecucaoFixture {
    private static final String SCHEMA = "rf010_browser_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void esquemaIsolado(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
    }

    @Test
    void motoristaIniciaFreteDePessoasERegistraOcorrenciaNoMesmoCiclo() {
        WebDriver driver = entrar("atual");
        try {
            driver.get(base + "/fretes/910061/execucao");
            clicar(driver, "Iniciar execução");
            db.sql("UPDATE frete SET motorista_atual_id = 910012, revisao_designacao = 2 WHERE id = 910061").update();
            clicar(driver, "Confirmar início");
            esperarTexto(driver, "Sua sessão ou designação não permite confirmar esta ação");
            assertThat(db.sql("SELECT count(*) FROM historico_execucao_frete WHERE frete_id = 910061")
                    .query(Integer.class).single()).isZero();
            db.sql("UPDATE frete SET motorista_atual_id = 910011, revisao_designacao = 3 WHERE id = 910061").update();
            clicar(driver, "Atualizar acompanhamento");
            clicar(driver, "Iniciar execução");
            js(driver).executeScript("""
                    const fetchOriginal = window.fetch.bind(window);
                    window.inicios = [];
                    window.fetch = async (url, opcoes) => {
                        const resposta = await fetchOriginal(url, opcoes);
                        if (String(url).endsWith('/execucao/inicio') && resposta.ok) {
                            window.inicios.push((await resposta.clone().json()).eventoId);
                            if (window.inicios.length === 1) throw new TypeError('Resposta perdida após commit');
                        }
                        return resposta;
                    };
                    """);
            clicar(driver, "Confirmar início");
            esperarTexto(driver, "A confirmação ficou sem resposta");
            clicar(driver, "Tentar novamente");
            esperarTexto(driver, "Execução iniciada.");
            assertThat(js(driver).executeScript("return inicios.length === 2 && inicios[0] === inicios[1]")).isEqualTo(true);
            clicar(driver, "Registrar ocorrência");
            preencher(driver, "Parada informada aos passageiros");
            clicar(driver, "Registrar ocorrência");
            esperarTexto(driver, "Ocorrência registrada.");
            driver.navigate().refresh();
            esperarTexto(driver, "FR-910061 · Em execução");
            esperarTexto(driver, "Parada informada aos passageiros");
            assertThat(db.sql("SELECT status FROM frete WHERE id = 910061").query(String.class).single())
                    .isEqualTo("EM_EXECUCAO");
            assertThat(db.sql("""
                    SELECT tipo_evento FROM historico_execucao_frete WHERE frete_id = 910061 ORDER BY id
                    """).query(String.class).list()).containsExactly("INICIO", "OCORRENCIA");
            assertThat(db.sql("""
                    SELECT count(*) FROM rf010_recibo_inicio i
                    JOIN rf010_recibo_ocorrencia o ON o.frete_id = i.frete_id AND o.ciclo_execucao_id = i.evento_id
                    WHERE i.frete_id = 910061
                    """).query(Integer.class).single()).isOne();
        } finally {
            driver.quit();
        }
    }
}
