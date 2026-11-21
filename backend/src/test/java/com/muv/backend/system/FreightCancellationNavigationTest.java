package com.muv.backend.system;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

// Start Vite (proxy /api -> localhost:8080) before running this system test.
@EnabledIfSystemProperty(named = "muv.rf011.e2e", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "selenium.headless=true")
@ActiveProfiles("rf011-test")
@Sql("/db/rf011-fixture.sql")
class FreightCancellationNavigationTest {
    private static final String SCHEMA = "rf011_browser_" + UUID.randomUUID().toString().replace("-", "");
    @DynamicPropertySource
    static void isolatedSchema(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
    }
    @Autowired ObjectProvider<WebDriver> drivers;
    @Autowired JdbcClient db;
    @Autowired PasswordEncoder passwords;
    @Value("${selenium.base-url:http://localhost:5173}") String base;

    @Test
    void validatesConfirmationCancelsAndShowsPersistedHistoryOnDesktopAndMobile() throws Exception {
        WebDriver driver = login("cliente");
        try {
            var wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            driver.manage().window().setSize(new Dimension(1351, 1040));
            driver.get(base + "/cliente/fretes/11010");
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Cancelar frete"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("justificativa")));
            assertThat(driver.findElement(By.linkText("Meus fretes")).getDomAttribute("href"))
                    .isEqualTo("/cliente/historico");
            assertThat(driver.findElement(By.cssSelector("[aria-label='Minha conta']")).getDomAttribute("href"))
                    .isEqualTo("/conta");
            driver.findElement(By.cssSelector("[aria-label='Notificações']")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                    "//*[@role='status' and contains(.,'central de notificações ainda não está disponível')]")));
            driver.findElement(By.cssSelector("[aria-label='Fechar aviso']")).click();
            var submit = driver.findElement(By.xpath("//button[normalize-space()='Confirmar cancelamento']"));
            var reason = driver.findElement(By.id("justificativa"));
            int initialHeight = reason.getRect().height;
            int registryFieldHeight = driver.findElement(By.cssSelector("main dd")).getRect().height;
            assertThat(Math.abs(initialHeight - registryFieldHeight)).isLessThanOrEqualTo(2);
            assertThat(submit.isEnabled()).isFalse();
            reason.sendKeys("   ");
            driver.findElement(By.cssSelector("input[type=checkbox]")).click();
            assertThat(submit.isEnabled()).isFalse();
            String longReason = "Detalhes do cancelamento.\n".repeat(8) + "Fim da justificativa.";
            reason.clear();
            reason.sendKeys(longReason);
            assertThat(reason.getRect().height).isGreaterThan(initialHeight);
            assertThat(reason.getDomProperty("value")).isEqualTo(longReason);
            assertThat(reason.getCssValue("outline-style")).isEqualTo("solid");
            reason.clear();
            reason.sendKeys("O envio foi adiado e a nova data ainda não foi definida.");
            assertThat(submit.isEnabled()).isTrue();
            driver.findElement(By.cssSelector("main h1")).click();
            screenshot(driver, "rf011-desktop.png");
            driver.manage().window().setSize(new Dimension(390, 844));
            var cards = driver.findElements(By.cssSelector("main section"));
            assertThat(cards.get(1).getRect().getY()).isGreaterThanOrEqualTo(cards.getFirst().getRect().getY()
                    + cards.getFirst().getRect().getHeight());
            var js = (JavascriptExecutor) driver;
            assertThat(js.executeScript(
                    "return document.documentElement.scrollWidth <= document.documentElement.clientWidth")).isEqualTo(true);
            // Use a taller capture at the same mobile width to show both cards.
            driver.manage().window().setSize(new Dimension(390, 1400));
            js.executeScript("window.scrollTo(0, 0)");
            screenshot(driver, "rf011-mobile.png");
            submit.click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@role='status' and contains(.,'sucesso')]")));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//p[contains(.,'O envio foi adiado')]")));
            assertThat(driver.findElements(By.id("justificativa"))).isEmpty();
            driver.navigate().refresh();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h1[.='Frete cancelado']")));
            assertThat(driver.findElements(By.id("justificativa"))).isEmpty();
            screenshot(driver, "rf011-success.png");
            driver.findElement(By.linkText("Voltar para meus fretes")).click();
            wait.until(ExpectedConditions.urlContains("frete=11010&periodo=2026-10"));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("freight-details")));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                    "//*[@id='freight-details']//*[normalize-space()='Frete cancelado']")));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                    "//main//*[normalize-space()='Cancelado']")));
            assertThat(driver.findElement(By.id("freight-details")).getText()).contains("O envio foi adiado");
            driver.get(base + "/cliente/fretes/11010");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//p[normalize-space()='Cancelado']")));
            assertThat(driver.findElements(By.linkText("Cancelar frete"))).isEmpty();
            assertThat(db.sql("SELECT usuario_destino_id FROM notificacao WHERE frete_id = 11010").query(Long.class).single()).isEqualTo(11002L);
            assertThat(db.sql("SELECT usuario_autor_id FROM historico_execucao_frete WHERE frete_id = 11010").query(Long.class).single()).isEqualTo(11001L);
        } finally { driver.quit(); }
    }

    @Test
    void rejectsStalePageAfterExecutionStarts() {
        WebDriver driver = login("prestador");
        try {
            var wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            openFreight(driver, wait);
            driver.findElement(By.id("justificativa")).sendKeys("Não será possível atender.");
            driver.findElement(By.cssSelector("input[type=checkbox]")).click();
            db.sql("UPDATE frete SET status = 'EM_EXECUCAO' WHERE id = 11010").update();
            driver.findElement(By.xpath("//button[normalize-space()='Confirmar cancelamento']")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@role='alert' and contains(.,'não pode ser cancelado')]")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("justificativa")));
            assertThat(db.sql("SELECT count(*) FROM historico_execucao_frete").query(Long.class).single()).isZero();
            assertThat(db.sql("SELECT count(*) FROM notificacao").query(Long.class).single()).isZero();
        } finally { driver.quit(); }
    }

    @Test
    void unrelatedAccountAndAnonymousCannotOpenCancellationForm() {
        WebDriver driver = login("terceiro");
        try {
            var wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            driver.get(base + "/fretes/11010/cancelar");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role=alert]")));
            assertThat(driver.findElements(By.id("justificativa"))).isEmpty();
            driver.manage().deleteAllCookies();
            driver.navigate().refresh();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Entrar")));
            assertThat(driver.findElements(By.id("justificativa"))).isEmpty();
        } finally { driver.quit(); }
    }

    private WebDriver login(String profile) {
        String email = profile + ".rf011@example.test";
        db.sql("UPDATE usuario SET senha = :password WHERE email = :email")
                .param("password", passwords.encode("TesteRF011!42")).param("email", email).update();
        WebDriver driver = drivers.getObject();
        try {
            var wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            driver.get(base + "/entrar");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys(email);
            driver.findElement(By.name("senha")).sendKeys("TesteRF011!42");
            driver.findElement(By.cssSelector("form button:not([type='button'])")).click();
            wait.until(ExpectedConditions.urlContains("/cliente/planejar-viagem"));
            return driver;
        } catch (RuntimeException error) { driver.quit(); throw error; }
    }

    private void openFreight(WebDriver driver, WebDriverWait wait) {
        driver.get(base + "/fretes/11010/cancelar");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("justificativa")));
    }

    private void screenshot(WebDriver driver, String name) throws Exception {
        Path target = Path.of("target", "rf011-browser", name);
        Files.createDirectories(target.getParent());
        Files.write(target, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }
}
