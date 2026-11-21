package com.muv.backend.system;

import com.muv.backend.dto.RespostaPrestador;
import com.muv.backend.service.PropostaService;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql("/db/rf007-solicitacoes.sql")
@Sql(scripts = {"/db/rf008-clean.sql", "/db/rf007-clean.sql"},
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ContratacaoSystemTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired PropostaService propostas;
    @Autowired ObjectProvider<WebDriver> drivers;
    @Value("${selenium.base-url}") String base;
    WebDriver driver;
    WebDriverWait wait;

    @BeforeEach
    void entrar() {
        jdbc.update("UPDATE usuario SET senha = ? WHERE id = 907001", encoder.encode("Teste-rf008"));
        driver = drivers.getObject();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.get(base + "/entrar");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email")))
                .sendKeys("cliente907@example.test");
        driver.findElement(By.name("senha")).sendKeys("Teste-rf008");
        driver.findElement(By.cssSelector("form button:not([type='button'])")).click();
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/entrar")));
    }

    @AfterEach
    void fechar() {
        if (driver != null) driver.quit();
    }

    @ParameterizedTest
    @ValueSource(longs = {907031, 907032})
    void confirmaEConsultaFrete(long solicitacao) {
        var oferta = propostas.avaliar(solicitacao, 907002);
        propostas.responder(solicitacao, 907002, new RespostaPrestador(RespostaPrestador.Tipo.MANTER,
                oferta.valorCliente(), oferta.condicoesCliente(), null, null, UUID.randomUUID()));
        driver.manage().window().setSize(solicitacao == 907032
                ? new Dimension(390, 844) : new Dimension(1440, 1100));
        jdbc.update("UPDATE solicitacao_frete SET data_hora_pretendida = '2026-11-20 08:00' WHERE id = ?", solicitacao);
        driver.get(base + "/cliente/historico");
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                "//button[@aria-pressed][contains(normalize-space(.), 'SL-" + solicitacao + "')]"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Comparar propostas"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space(.)='Ver proposta']"))).click();
        By confirmar = By.xpath("//button[starts-with(normalize-space(.), 'Confirmar contratação')]");
        assertFalse(driver.findElement(confirmar).isEnabled());
        driver.findElement(By.cssSelector("input[type=checkbox]")).click();
        wait.until(ExpectedConditions.elementToBeClickable(confirmar)).click();
        wait.until(ExpectedConditions.urlContains("/cliente/fretes/"));
        conferirFrete();
        assertEquals(Boolean.TRUE, ((JavascriptExecutor) driver).executeScript(
                "return document.documentElement.scrollWidth <= window.innerWidth"));
        driver.navigate().refresh();
        conferirFrete();
        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM notificacao n
                JOIN frete f ON f.id = n.frete_id
                WHERE f.solicitacao_id = ? AND n.usuario_destino_id = 907002
                """, Integer.class, solicitacao));
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Ver histórico da solicitação"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Ver frete contratado"))).click();
        conferirFrete();
        assertTrue(driver.manage().logs().get(LogType.BROWSER).getAll().stream()
                .noneMatch(entry -> entry.getLevel() == java.util.logging.Level.SEVERE
                        && !entry.getMessage().contains("favicon")));
    }

    private void conferirFrete() {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("main h1"), "Frete contratado"));
        assertTrue(driver.findElement(By.tagName("main")).getText().contains("Aguardando designação"));
    }
}
