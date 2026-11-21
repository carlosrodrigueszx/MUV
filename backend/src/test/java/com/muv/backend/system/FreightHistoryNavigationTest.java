package com.muv.backend.system;

import com.muv.backend.config.SeleniumConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(classes = SeleniumConfig.class,
        initializers = ConfigDataApplicationContextInitializer.class)
@ActiveProfiles("dev")
class FreightHistoryNavigationTest {

    @Autowired
    private ObjectProvider<WebDriver> drivers;

    @Value("${selenium.base-url:http://localhost:5173}")
    private String baseUrl;

    @Test
    void authenticatedClientConsultsHistoryUsingSessionCookie() {
        String email = requiredEnvironment("MUV_E2E_EMAIL");
        String password = requiredEnvironment("MUV_E2E_PASSWORD");
        consultsHistory(email, password, "cliente", "Cliente");
    }

    @Test
    void authenticatedProviderConsultsHistoryUsingSessionCookie() {
        String email = requiredEnvironment("MUV_E2E_PROVIDER_EMAIL");
        String password = requiredEnvironment("MUV_E2E_PROVIDER_PASSWORD");
        consultsHistory(email, password, "prestador", "Prestador");
    }

    private void consultsHistory(String email, String password, String profilePath,
            String expectedProfileLabel) {
        String base = baseUrl.replaceAll("/+$", "");
        WebDriver driver = drivers.getObject();

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            driver.get(base + "/entrar");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys(email);
            driver.findElement(By.name("senha")).sendKeys(password);
            driver.findElement(By.cssSelector("form button:not([type='button'])")).click();
            wait.until(ExpectedConditions.urlToBe(base + "/cliente/planejar-viagem"));

            if ("cliente".equals(profilePath)) {
                driver.get(base + "/cliente/prestadores");
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("a[href='/cliente/historico']"))).click();
            } else {
                driver.get(base + "/" + profilePath + "/historico");
            }
            wait.until(ExpectedConditions.urlToBe(base + "/" + profilePath + "/historico"));
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h1[normalize-space()='Seu histórico de fretes']")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.xpath("//*[normalize-space()='Consultando o histórico de fretes...']")));

            assertTrue(driver.findElements(By.cssSelector("[role='alert']")).isEmpty(),
                    "A consulta autenticada não deve exibir erro de acesso.");
            var profile = new Select(driver.findElement(
                    By.xpath("//label[span[normalize-space()='Perfil']]/select")));
            assertTrue(profile.getOptions().size() == 1
                            && expectedProfileLabel.equals(profile.getFirstSelectedOption().getText()),
                    "A conta deve visualizar somente os perfis que possui.");
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h2[contains(normalize-space(), 'registro') and contains(normalize-space(), 'encontrado')]")));

            driver.findElement(By.xpath("//button[normalize-space()='Filtrar']")).click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.xpath("//*[normalize-space()='Consultando o histórico de fretes...']")));
            assertTrue(driver.findElements(By.cssSelector("[role='alert']")).isEmpty(),
                    "O filtro deve preservar a sessão autenticada.");
        } finally {
            driver.quit();
        }
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Configure " + name + " com uma conta exclusiva para testes.");
        }
        return value;
    }
}
