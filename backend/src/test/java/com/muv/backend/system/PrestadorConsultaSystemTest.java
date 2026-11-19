package com.muv.backend.system;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.muv.backend.testutil.PrestadorConsultaFixtures;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PrestadorConsultaSystemTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectProvider<WebDriver> drivers;
    @Value("${selenium.base-url:http://localhost:5173}") private String baseUrl;
    @Value("${rf005.evidence-dir:../docs/testes/evidencias/RF005}") private String evidenceDirectory;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        PrestadorConsultaFixtures.seed(jdbc);
        driver = drivers.getObject();
        driver.manage().window().setSize(new Dimension(1365, 1020));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) driver.quit();
        PrestadorConsultaFixtures.clean(jdbc);
    }

    @Test
    void searchesByPersonModalityAndFortalezaAndDisplaysDatabaseInformation() throws IOException {
        openSearch();
        new Select(driver.findElement(By.id("service"))).selectByValue("PESSOA");
        driver.findElement(By.id("region")).sendKeys("Fortaleza, CE");
        search();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("main > h2"), "1 prestador atende sua rota"));
        String card = driver.findElement(By.id("prestador-991001")).getText();
        assertTrue(card.contains("Autônomo Horizonte Teste"));
        assertTrue(card.contains("Autônomo"));
        assertTrue(card.contains("Autorizado para Pessoas"));
        assertTrue(card.contains("Fortaleza - CE"));
        assertTrue(card.contains("MUV Micro-ônibus Ativo Teste (2024)"));
        assertTrue(card.contains("Até 20 passageiros"));
        assertTrue(!card.contains("Van Inativa Teste"));
        saveScreenshot("rf005-busca-pessoa-fortaleza-autonomo.png");
    }

    @Test
    void searchesByEncomendaAndRecifeAndDisplaysFictionalCooperativeCompany() throws IOException {
        openSearch();
        new Select(driver.findElement(By.id("service"))).selectByValue("ENCOMENDA");
        driver.findElement(By.id("region")).sendKeys("Recife, PE");
        new Select(driver.findElement(By.id("provider-type"))).selectByValue("EMPRESA");
        search();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("main > h2"), "1 prestador atende sua rota"));
        String card = driver.findElement(By.id("prestador-991003")).getText();
        assertTrue(card.contains("Cooperativa Rota Azul Fictícia"));
        assertTrue(card.contains("Empresa"));
        assertTrue(card.contains("Autorizado para Encomendas"));
        assertTrue(card.contains("Recife - PE"));
        assertTrue(card.contains("MUV Furgão Cooperativa Teste (2025)"));
        assertTrue(card.contains("Até 850 kg de carga"));
        saveScreenshot("rf005-busca-encomenda-recife-cooperativa-ficticia.png");
    }

    @Test
    void displaysInformativeMessageWhenSearchHasNoResults() throws IOException {
        openSearch();
        driver.findElement(By.id("region")).sendKeys("Natal, RN");
        search();

        By noResults = By.xpath("//*[normalize-space()='Nenhum prestador encontrado para os filtros informados.']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(noResults));
        saveScreenshot("rf005-sem-resultados-natal-rn.png");
    }

    private void openSearch() {
        driver.get(baseUrl.replaceAll("/+$", "") + "/cliente/prestadores");
    }

    private void search() {
        driver.findElement(By.xpath("//button[normalize-space()='Pesquisar']")).click();
    }

    private void saveScreenshot(String fileName) throws IOException {
        Path destination = Path.of(evidenceDirectory);
        Files.createDirectories(destination);
        Path screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
        Files.copy(screenshot, destination.resolve(fileName),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
