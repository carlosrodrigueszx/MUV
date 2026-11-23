package com.muv.backend.system;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"selenium.browser=${SELENIUM_BROWSER:chrome}", "selenium.headless=true"})
@ActiveProfiles("rf001-local")
@DirtiesContext
@Sql(scripts = {"/db/rf009-clean.sql", "/db/rf009-designacao.sql"})
@Sql(scripts = "/db/rf009-clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DesignacaoSystemTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectProvider<WebDriver> drivers;
    @Value("${selenium.base-url:http://localhost:5173}") String base;

    WebDriver driver;
    WebDriverWait wait;

    @BeforeEach
    void entrar() {
        jdbc.update("UPDATE usuario SET senha = ? WHERE id = 909002", encoder.encode("Teste-rf009"));
        driver = drivers.getObject();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.manage().window().setSize(new Dimension(1440, 1100));
        driver.get(base + "/entrar");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email")))
                .sendKeys("empresa909@example.test");
        driver.findElement(By.name("senha")).sendKeys("Teste-rf009");
        driver.findElement(By.cssSelector("form button:not([type='button'])")).click();
        wait.until(ExpectedConditions.not(ExpectedConditions.urlContains("/entrar")));
    }

    @AfterEach
    void fechar() {
        if (driver != null) driver.quit();
    }

    @Test
    void designaTrataConflitoESubstituiComPersistenciaReal() throws Exception {
        driver.get(base + "/prestador/fretes/909060/designacao");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("main h1"), "Designe a equipe"));
        assertThat(driver.findElement(By.cssSelector("option[value='909021']")).isEnabled()).isFalse();
        salvarImagem("designacao-desktop");

        driver.manage().window().setSize(new Dimension(390, 844));
        assertThat((Boolean) ((JavascriptExecutor) driver).executeScript(
                "return document.documentElement.scrollWidth <= window.innerWidth")).isTrue();
        salvarImagem("designacao-estreita");
        driver.manage().window().setSize(new Dimension(1440, 1100));

        selecionar(0, "909010");
        selecionar(1, "909020");
        jdbc.update("UPDATE frete SET revisao_designacao = 1 WHERE id = 909060");
        clicar("Salvar designação");
        wait.until(ExpectedConditions.textMatches(By.cssSelector("main [role='alert']"),
                Pattern.compile("foi atualizad[oa]")));
        clicar("Salvar designação");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("main [role='status']"), "Designação salva"));

        assertThat(jdbc.queryForMap("""
                SELECT status, motorista_atual_id, veiculo_atual_id, revisao_designacao
                FROM frete WHERE id = 909060
                """))
                .containsEntry("status", "PRONTO_INICIO")
                .containsEntry("motorista_atual_id", 909010L)
                .containsEntry("veiculo_atual_id", 909020L)
                .containsEntry("revisao_designacao", 2L);

        driver.get(base + "/prestador/fretes/909060/substituicao");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("main h1"), "Substitua motorista"));
        selecionar(0, "909018");
        assertThat(driver.findElement(By.xpath(
                "//main//button[normalize-space(.)='Confirmar substituição']")).isEnabled()).isFalse();
        driver.findElement(By.tagName("textarea")).sendKeys("Ajuste operacional da equipe");
        jdbc.update("UPDATE frete SET revisao_designacao = 3 WHERE id = 909060");
        clicar("Confirmar substituição");
        wait.until(ExpectedConditions.textMatches(By.cssSelector("main [role='alert']"),
                Pattern.compile("foi atualizad[oa]")));
        clicar("Confirmar substituição");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("main [role='status']"), "cliente notificado"));
        salvarImagem("substituicao-desktop");

        assertThat(jdbc.queryForMap("""
                SELECT status, motorista_atual_id, veiculo_atual_id, revisao_designacao
                FROM frete WHERE id = 909060
                """))
                .containsEntry("status", "PRONTO_INICIO")
                .containsEntry("motorista_atual_id", 909018L)
                .containsEntry("veiculo_atual_id", 909020L)
                .containsEntry("revisao_designacao", 4L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM historico_execucao_frete
                WHERE frete_id = 909060
                  AND tipo_evento IN ('DESIGNACAO', 'SUBSTITUICAO_SEM_ALTERACAO')
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notificacao
                WHERE frete_id = 909060 AND usuario_destino_id = 909001
                """, Integer.class)).isEqualTo(1);
    }

    private void selecionar(int indice, String valor) {
        var seletores = driver.findElements(By.cssSelector("main select"));
        new Select(seletores.get(indice)).selectByValue(valor);
    }

    private void clicar(String texto) {
        WebElement botao = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//main//button[normalize-space(.)='" + texto + "']")));
        botao.click();
    }

    private void salvarImagem(String nome) throws Exception {
        Path pasta = Path.of("target", "rf009-evidencias");
        Files.createDirectories(pasta);
        Files.write(pasta.resolve(nome + ".png"),
                ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }
}
