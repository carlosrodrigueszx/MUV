package com.muv.backend.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.muv.backend.testutil.PrestadorConsultaFixtures;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.Color;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TripPlanningSystemTest {
    private static final long PASSENGER_ID = 991030;
    private static final String EMAIL = "planejamento@example.test";
    private static final String PASSWORD = "ViagemTeste!123";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ObjectProvider<WebDriver> drivers;
    @Value("${selenium.base-url:http://localhost:5173}") private String baseUrl;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void openAuthenticatedPlanner() {
        PrestadorConsultaFixtures.seed(jdbc);
        jdbc.update("UPDATE autorizacao_modalidade SET status = 'APROVADO' WHERE id = 991002");
        jdbc.update("UPDATE prestador_empresa SET nome_fantasia = 'A Empresa Teste' WHERE id = 991002");
        jdbc.update("INSERT INTO usuario (id, nome, telefone, email, senha) VALUES (?, ?, ?, ?, ?)",
                PASSENGER_ID, "Passageiro Teste", "85990001030", EMAIL, passwords.encode(PASSWORD));
        jdbc.update("INSERT INTO passageiro (id, cpf) VALUES (?, ?)", PASSENGER_ID, "99103000000");

        driver = drivers.getObject();
        driver.manage().window().setSize(new Dimension(1365, 900));
        wait = new WebDriverWait(driver, Duration.ofSeconds(12));
        driver.get(baseUrl + "/entrar");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys(EMAIL);
        driver.findElement(By.name("senha")).sendKeys(PASSWORD);
        driver.findElement(By.cssSelector("form button:not([type='button'])")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("embarque")));
    }

    @AfterEach
    void clean() {
        try {
            if (driver != null) driver.quit();
        } finally {
            jdbc.update("DELETE FROM passageiro WHERE id = ?", PASSENGER_ID);
            jdbc.update("DELETE FROM usuario WHERE id = ?", PASSENGER_ID);
            PrestadorConsultaFixtures.clean(jdbc);
        }
    }

    @Test
    void preservesRoundTripStopsAndOfferAndReviewsOneRecipient() {
        fillTrip(12);
        driver.findElement(By.xpath("//label[normalize-space()='Ida e volta']")).click();
        setField("dataRetorno", LocalDate.now().plusDays(2).toString());
        setField("horaRetorno", "16:00");
        for (String stop : new String[]{"Primeira parada", "Parada a remover", "Última parada"}) {
            clickButton("Adicionar outra parada");
            var stops = driver.findElements(By.name("parada"));
            stops.getLast().sendKeys(stop);
        }
        driver.findElement(By.cssSelector("button[aria-label='Remover parada 2']")).click();
        setField("valor", "250.50");
        driver.findElement(By.name("condicoes")).sendKeys("Inclui pedágios");
        clickButton("Consultar prestadores");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("prestador-991001")));
        String summary = driver.findElement(By.cssSelector("[aria-labelledby='trip-summary']")).getText();
        assertTrue(summary.contains("Ida e volta"));
        assertTrue(summary.contains("16:00"));
        assertTrue(summary.indexOf("Primeira parada") < summary.indexOf("Última parada"));
        assertFalse(summary.contains("Parada a remover"));
        assertTrue(summary.contains("Inclui pedágios"));
        assertTrue(summary.contains("12 passageiros"));
        assertTrue(driver.findElement(By.tagName("main")).getText().contains(
                "A disponibilidade para o período precisa ser confirmada pelo prestador."));

        new Select(driver.findElement(By.xpath("//label[contains(., 'Ordenar por')]/select"))).selectByValue("nome");
        assertEquals("prestador-991002", driver.findElements(By.cssSelector("article[id^='prestador-']"))
                .getFirst().getDomAttribute("id"));

        selectProvider(991001);
        selectProvider(991002);
        assertEquals(1, driver.findElements(By.cssSelector("button[aria-pressed='true']")).size());
        assertTrue(driver.findElement(By.id("request-review")).getText().contains("Revisar"));
        assertTrue(driver.findElement(By.cssSelector("[aria-labelledby='request-review']")).getText()
                .contains("Destinatário: A Empresa Teste"));
        assertFalse(driver.findElement(button("Enviar solicitação direcionada")).isEnabled());
        assertTrue(driver.findElements(By.xpath("//button[normalize-space()='Criar solicitação aberta']")).isEmpty());

        driver.navigate().back();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("embarque")));
        assertEquals("Fortaleza", fieldValue("embarque"));
        assertEquals("16:00", fieldValue("horaRetorno"));
        assertEquals("250.50", fieldValue("valor"));
        assertEquals("Inclui pedágios", fieldValue("condicoes"));
        assertEquals(2, driver.findElements(By.name("parada")).size());

        clickButton("Consultar prestadores");
        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Editar itinerário"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("embarque")));
        assertEquals("Natal", fieldValue("destino"));
        assertEquals("12", fieldValue("passageiros"));
    }

    @Test
    void validatesDepartureReturnAndPartialOfferThenSearchesOneWayWithoutOffer() {
        driver.manage().window().setSize(new Dimension(390, 850));
        assertPageFitsViewport();
        fillTrip(20);
        setField("dataIda", LocalDate.now().minusDays(1).toString());
        clickButton("Consultar prestadores");
        assertAlert("Informe uma data e um horário de ida futuros.");
        setField("dataIda", LocalDate.now().plusDays(1).toString());

        driver.findElement(By.xpath("//label[normalize-space()='Ida e volta']")).click();
        setField("dataRetorno", fieldValue("dataIda"));
        setField("horaRetorno", "08:00");
        clickButton("Consultar prestadores");
        assertAlert("O retorno deve ocorrer depois da partida.");
        driver.findElement(By.xpath("//label[normalize-space()='Ida']")).click();

        setField("valor", ".50");
        clickButton("Consultar prestadores");
        assertAlert("Informe as condições da oferta.");
        assertEquals("condicoes", driver.switchTo().activeElement().getDomAttribute("name"));
        setField("valor", "");

        clickButton("Consultar prestadores");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("prestador-991001")));
        assertTrue(driver.findElements(By.id("prestador-991002")).isEmpty());
        String summary = driver.findElement(By.cssSelector("[aria-labelledby='trip-summary']")).getText();
        assertFalse(summary.contains("Retorno"));
        assertFalse(summary.contains("Oferta original"));

        assertPageFitsViewport();
        driver.findElement(By.cssSelector("details summary")).sendKeys(Keys.ENTER);
        assertFalse(driver.findElement(By.id("region")).isDisplayed());
        driver.findElement(By.cssSelector("details summary")).sendKeys(Keys.ENTER);
        driver.manage().window().setSize(new Dimension(820, 850));
        assertPageFitsViewport();
        driver.findElement(By.id("region")).sendKeys("Fortaleza, CE");
        new Select(driver.findElement(By.id("provider-type"))).selectByValue("EMPRESA");
        clickButton("Pesquisar");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.tagName("main"), "Nenhum prestador encontrado"));
        clickButton("Limpar filtros");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("prestador-991001")));
    }

    @Test
    void movesTheActiveNavigationIndicatorBetweenPlannerAndProviders() {
        var planner = By.cssSelector("nav a[aria-current='page']");
        assertEquals("Planejar viagem", driver.findElement(planner).getText());
        assertEquals("#176457", Color.fromString(driver.findElement(planner).getCssValue("border-bottom-color")).asHex());

        fillTrip(12);
        clickButton("Consultar prestadores");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(planner, "Prestadores"));
        var activeBar = driver.findElement(By.cssSelector("nav a[aria-current='page'] span:last-child"));
        assertEquals("#17695b", Color.fromString(activeBar.getCssValue("background-color")).asHex());
        assertEquals(3, activeBar.getSize().getHeight());
        var inactiveBar = driver.findElement(By.cssSelector("nav a[href='/'] span:last-child"));
        assertEquals(Color.fromString("transparent"), Color.fromString(inactiveBar.getCssValue("background-color")));

        wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Editar itinerário"))).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(planner, "Planejar viagem"));
        assertEquals("#176457", Color.fromString(driver.findElement(planner).getCssValue("border-bottom-color")).asHex());
    }

    private void assertPageFitsViewport() {
        assertEquals(true, ((JavascriptExecutor) driver).executeScript("""
                return document.documentElement.scrollWidth <= document.documentElement.clientWidth;
                """));
    }

    private void fillTrip(int passengers) {
        driver.findElement(By.name("embarque")).sendKeys("Fortaleza");
        driver.findElement(By.name("destino")).sendKeys("Natal");
        setField("dataIda", LocalDate.now().plusDays(1).toString());
        setField("horaIda", "08:00");
        setField("passageiros", Integer.toString(passengers));
    }

    private void setField(String name, String value) {
        ((JavascriptExecutor) driver).executeScript("""
                const [input, value] = arguments;
                Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value").set.call(input, value);
                input.dispatchEvent(new Event("input", { bubbles: true }));
                """, driver.findElement(By.name(name)), value);
    }

    private String fieldValue(String name) {
        return driver.findElement(By.name(name)).getDomProperty("value");
    }

    private void assertAlert(String message) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("form [role='alert']"), message));
    }

    private void selectProvider(int id) {
        driver.findElement(By.cssSelector("#prestador-" + id + " button[aria-pressed]")).click();
    }

    private void clickButton(String label) {
        wait.until(ExpectedConditions.elementToBeClickable(button(label))).click();
    }

    private By button(String label) {
        return By.xpath("//button[normalize-space()='" + label + "']");
    }
}
