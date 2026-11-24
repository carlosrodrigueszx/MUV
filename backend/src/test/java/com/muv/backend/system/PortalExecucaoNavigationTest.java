package com.muv.backend.system;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

@EnabledIfSystemProperty(named = "muv.rf010.e2e", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "selenium.headless=true")
@ActiveProfiles("rf010-test")
@Sql(scripts = {"/db/rf010-consulta.sql", "/db/rf010-navegacao-portal.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class PortalExecucaoNavigationTest extends NavegacaoExecucaoFixture {
    private static final String SCHEMA = "rf010_portal_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void esquemaIsolado(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
    }

    @Test
    void abreFreteProntoMesmoForaDosQuatroItensDaAgenda() {
        WebDriver motorista = entrar("atual");
        try {
            var agenda = espera(motorista).until(ExpectedConditions.visibilityOfElementLocated(By.id("agenda")));
            espera(motorista).until(ExpectedConditions.numberOfElementsToBe(By.cssSelector("#agenda li"), 4));
            assertThat(agenda.findElements(By.tagName("li"))).allSatisfy(item ->
                    assertThat(item.getText()).contains("Santos → Fortaleza"));
            abrirFrete(motorista, 910061, "Pronto para início", "Iniciar execução");
        } finally {
            motorista.quit();
        }
    }

    @Test
    void abreFretesEmExecucaoEInterrompidoComPartidaPassada() {
        WebDriver motorista = entrar("atual");
        try {
            abrirFrete(motorista, 910063, "Em execução", "Registrar ocorrência", "Registrar interrupção");
            motorista.navigate().back();
            abrirFrete(motorista, 910062, "Interrompido", "Retomar execução", "Encerrar sem conclusão");
        } finally {
            motorista.quit();
        }
    }

    @Test
    void colegaSemDesignacaoNaoRecebeAtalhoNemAcessoDireto() {
        WebDriver colega = entrar("colega");
        try {
            var totalAtivos = By.xpath("//main//p[normalize-space()='Fretes ativos']/../following-sibling::p");
            espera(colega).until(ExpectedConditions.textToBe(totalAtivos, "0"));
            assertThat(colega.findElements(By.cssSelector("main a[href$='/execucao']"))).isEmpty();

            for (long id : new long[]{910061, 910062, 910063}) {
                colega.get(base + rota(id));
                var alerta = espera(colega).until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("main [role='alert']")));
                assertThat(alerta.getText()).isEqualTo("Você não tem acesso a este frete.");
            }
        } finally {
            colega.quit();
        }
    }

    private void abrirFrete(WebDriver motorista, long id, String estado, String... acoes) {
        String rota = rota(id);
        var atalho = espera(motorista).until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("section[aria-labelledby='overview-execution'] a[href='" + rota + "']")));
        assertThat(atalho.getText()).isEqualTo("Acompanhar execução");
        atalho.click();
        espera(motorista).until(ExpectedConditions.urlToBe(base + rota));
        esperarTexto(motorista, "FR-" + id + " · " + estado);
        for (String acao : acoes) {
            espera(motorista).until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//main//button[normalize-space()='" + acao + "']")));
        }
    }

    private static String rota(long id) {
        return "/fretes/" + id + "/execucao";
    }
}
