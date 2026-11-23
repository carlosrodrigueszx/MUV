package com.muv.backend.system;

import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.web.resources.static-locations=file:../frontend/dist/")
@ActiveProfiles("test")
@Import(SolicitacoesDisponiveisSystemTest.Spa.class)
@Sql("/db/rf007-solicitacoes.sql")
@Sql(scripts="/db/rf007-clean.sql", executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SolicitacoesDisponiveisSystemTest {
    @TestConfiguration
    static class Spa implements WebMvcConfigurer {
        @Override public void addViewControllers(ViewControllerRegistry registry) {
            registry.addViewController("/entrar").setViewName("forward:/index.html");
            registry.addViewController("/prestador/solicitacoes").setViewName("forward:/index.html");
            registry.addViewController("/prestador/solicitacoes/{id}/avaliacao").setViewName("forward:/index.html");
        }
    }
    @LocalServerPort int porta;
    @Autowired ObjectProvider<WebDriver> drivers;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    @Test void filtraEAbreAvaliacaoDaRf007() throws Exception {

        jdbc.update("UPDATE usuario SET senha=? WHERE id=907002", encoder.encode("Teste-rf006"));
        WebDriver navegador = drivers.getObject();
        try {
            jdbc.update("INSERT INTO regiao_atuacao(id,cidade,estado,id_prestador) VALUES (9067001,'Crato','CE',907002)");
            var espera = new WebDriverWait(navegador, Duration.ofSeconds(15));
            String base = "http://localhost:" + porta;
            navegador.get(base + "/entrar");
            espera.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys("autor907@example.test");
            navegador.findElement(By.name("senha")).sendKeys("Teste-rf006");
            navegador.findElement(By.cssSelector("form button:not([type='button'])")).click();
            espera.until(d -> !d.getCurrentUrl().endsWith("/entrar"));
            navegador.get(base + "/prestador/solicitacoes");
            espera.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("main"), "907031"));
            assertTrue(navegador.findElement(By.tagName("main")).getText().contains("907032"));
            navegador.manage().window().setSize(new Dimension(1440, 1252));
            var painel = By.cssSelector("section[aria-label='Perfil de atuação']");
            espera.until(ExpectedConditions.textToBePresentInElementLocated(painel, "Pessoas e encomendas · Autorizadas"));
            assertTrue(navegador.findElement(painel).getText().contains("Prestador RF007"));
            assertTrue(navegador.findElement(painel).getText().contains("Crato/CE"));
            Path pasta = Files.createDirectories(Path.of("target", "rf006-evidencias"));
            Files.write(pasta.resolve("desktop.png"), ((TakesScreenshot) navegador).getScreenshotAs(OutputType.BYTES));
            navegador.manage().window().setSize(new Dimension(390, 844));
            assertEquals(true, ((JavascriptExecutor) navegador).executeScript(
                    "return document.documentElement.scrollWidth <= document.documentElement.clientWidth"));
            Files.write(pasta.resolve("mobile.png"), ((TakesScreenshot) navegador).getScreenshotAs(OutputType.BYTES));
            navegador.findElement(By.cssSelector("select")).sendKeys("Encomendas");
            navegador.findElement(By.xpath("//button[normalize-space()='Aplicar filtros']")).click();
            espera.until(d -> !d.findElement(By.tagName("main")).getText().contains("907031"));
            assertTrue(navegador.findElement(By.tagName("main")).getText().contains("907032"));
            var cidade = navegador.findElement(By.cssSelector("input[list='regioes-rf006']"));
            cidade.sendKeys("Crato");
            navegador.findElement(By.xpath("//button[normalize-space()='Aplicar filtros']")).click();
            espera.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("main"), "907032"));
            cidade.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"), org.openqa.selenium.Keys.BACK_SPACE);
            cidade.sendKeys("Cidade sem pedidos");
            navegador.findElement(By.xpath("//button[normalize-space()='Aplicar filtros']")).click();
            espera.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("[role='status']"), "Nenhuma solicitação"));
            cidade.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"), org.openqa.selenium.Keys.BACK_SPACE);
            navegador.findElement(By.xpath("//button[normalize-space()='Aplicar filtros']")).click();
            espera.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("main"), "907032"));
            navegador.findElement(By.linkText("Ver e apresentar proposta")).click();
            espera.until(d -> d.getCurrentUrl().contains("/prestador/solicitacoes/907032/avaliacao"));
            espera.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".rf-preco"), "198,46"));
            var menu = By.cssSelector("aside nav a[aria-current='page'][href='/prestador/solicitacoes']");
            espera.until(ExpectedConditions.textToBePresentInElementLocated(menu, "2"));
            espera.until(ExpectedConditions.elementToBeClickable(menu)).click();
            espera.until(ExpectedConditions.urlToBe(base + "/prestador/solicitacoes"));
            espera.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("main"), "907032"));
        } finally {
            jdbc.update("DELETE FROM regiao_atuacao WHERE id=9067001 AND id_prestador=907002");
            navegador.quit();
        }
    }

    @Test void excluiPedidoDirecionadoAOutroPrestador() {
        jdbc.update("UPDATE usuario SET senha=? WHERE id=907003", encoder.encode("Teste-rf006"));
        WebDriver navegador = drivers.getObject();
        try {
            var espera = new WebDriverWait(navegador, Duration.ofSeconds(15));
            String base = "http://localhost:" + porta;
            navegador.get(base + "/entrar");
            espera.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys("empresa907@example.test");
            navegador.findElement(By.name("senha")).sendKeys("Teste-rf006");
            navegador.findElement(By.cssSelector("form button:not([type='button'])")).click();
            espera.until(d -> !d.getCurrentUrl().endsWith("/entrar"));
            navegador.get(base + "/prestador/solicitacoes");
            espera.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("main"), "907031"));
            assertFalse(navegador.findElement(By.tagName("main")).getText().contains("907032"));
            assertFalse(navegador.findElement(By.tagName("main")).getText().contains("cliente907@example.test"));
        } finally {
            navegador.quit();
        }
    }
}
