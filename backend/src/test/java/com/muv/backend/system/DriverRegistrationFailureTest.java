package com.muv.backend.system;

import com.muv.backend.config.SeleniumConfig;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = SeleniumConfig.class,
        initializers = ConfigDataApplicationContextInitializer.class)
@ActiveProfiles("test")
public class DriverRegistrationFailureTest {
    private static final String OPERATION_FAILED = "Não foi possível concluir a operação.";

    @Autowired
    private ObjectProvider<WebDriver> drivers;

    @Value("${selenium.base-url:http://localhost:5173}")
    private String baseUrl;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        // O bean prototype fornece uma sessão nova, sem login anterior.
        driver = drivers.getObject();
        driver.manage().window().setSize(new Dimension(1365, 1020));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void showsOperationFailureWhenRegisteringDriverWithoutSession() {
        driver.get(baseUrl.replaceAll("/+$", "") + "/prestador/motoristas");
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Novo motorista']"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[role='dialog']")));

        fill("name", "Motorista de Teste RF003");
        fill("birth", "01/01/2000");
        new Select(driver.findElement(field("gender"))).selectByValue("masculino");
        fill("cpf", "12345678909");
        fill("cnh", "12345678901");
        new Select(driver.findElement(field("category"))).selectByValue("D");
        fill("phone", "85900000000");
        fill("email", "rf003-sem-login@example.test");
        fill("password", "SenhaTeste!123");
        fill("password-confirmation", "SenhaTeste!123");

        wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("[role='dialog'] button[type='submit']"))).click();

        By error = By.cssSelector("[role='dialog'] [role='alert']");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(error, OPERATION_FAILED));
        assertEquals(OPERATION_FAILED, driver.findElement(error).getText());
        assertTrue(driver.findElement(By.cssSelector("[role='dialog']")).isDisplayed(),
                "O formulário deve permanecer aberto quando a operação de cadastro falha.");
    }

    private By field(String name) {
        // useId() altera o prefixo entre renderizações; o sufixo identifica o campo.
        return By.cssSelector("[role='dialog'] [id$='-" + name + "']");
    }

    private void fill(String name, String value) {
        var input = wait.until(ExpectedConditions.visibilityOfElementLocated(field(name)));
        input.clear();
        input.sendKeys(value);
    }
}
