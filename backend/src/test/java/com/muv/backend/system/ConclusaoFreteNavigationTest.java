package com.muv.backend.system;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

@EnabledIfSystemProperty(named = "muv.rf012.e2e", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = "selenium.headless=true")
@ActiveProfiles("rf010-test")
@Sql(scripts = "/db/rf010-consulta.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class ConclusaoFreteNavigationTest extends NavegacaoExecucaoFixture {
    private static final String SCHEMA = "rf012_browser_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void esquemaIsolado(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
    }

    @Test
    void motoristaConcluiEncomendaEClienteConsultaResultado() {
        db.sql("ALTER SEQUENCE historico_execucao_frete_id_seq RESTART WITH 910075").update();
        WebDriver motorista = entrar("atual");
        try {
            motorista.manage().window().setSize(new Dimension(390, 900));
            motorista.get(base + "/fretes/910063/execucao");
            clicar(motorista, "Registrar conclusão");
            js(motorista).executeScript("""
                    document.querySelector('[name=data]').value = arguments[0];
                    document.querySelector('[name=horario]').value = '10:30';
                    """, LocalDate.now(ZoneId.of("America/Fortaleza")).minusDays(1).toString());
            motorista.findElement(By.name("nomeRecebedor")).sendKeys("Recebedor Teste");
            clicar(motorista, "Confirmar conclusão");
            esperarTexto(motorista, "Conclusão registrada.");
            motorista.navigate().refresh();
            esperarTexto(motorista, "Frete concluído");
        } finally {
            motorista.quit();
        }

        assertThat(db.sql("SELECT status FROM frete WHERE id = 910063").query(String.class).single())
                .isEqualTo("CONCLUIDO");
        assertThat(db.sql("SELECT cpf_destinatario IS NULL FROM frete WHERE id = 910063")
                .query(Boolean.class).single()).isTrue();
        WebDriver cliente = entrar("cliente");
        try {
            cliente.get(base + "/cliente/fretes/910063");
            esperarTexto(cliente, "Recebido por Recebedor Teste");
            assertThat(cliente.findElement(By.tagName("main")).getText()).doesNotContain("52998224725");
        } finally {
            cliente.quit();
        }
    }
}
