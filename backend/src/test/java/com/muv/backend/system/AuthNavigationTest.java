package com.muv.backend.system;

import com.muv.backend.config.SeleniumConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = SeleniumConfig.class,
        initializers = ConfigDataApplicationContextInitializer.class)
@ActiveProfiles("dev")
class AuthNavigationTest {
    @Autowired
    private ObjectProvider<WebDriver> drivers;

    @Value("${selenium.base-url:http://localhost:5173}")
    private String baseUrl;

    @Test
    void opensPlannerAfterLoginAndProtectsItAfterLogout() {
        String email = System.getenv("MUV_E2E_EMAIL");
        String password = System.getenv("MUV_E2E_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Configure MUV_E2E_EMAIL e MUV_E2E_PASSWORD com uma conta de teste.");
        }

        String base = baseUrl.replaceAll("/+$", "");
        String planner = base + "/cliente/planejar-viagem";
        WebDriver driver = drivers.getObject();
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            driver.get(base + "/entrar");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys(email);
            driver.findElement(By.name("senha")).sendKeys(password);
            driver.findElement(By.cssSelector("form button:not([type='button'])")).click();

            wait.until(ExpectedConditions.urlToBe(planner));
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//h2[normalize-space()='Defina seu roteiro']")));
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[aria-label='Minha conta']"))).click();
            wait.until(ExpectedConditions.urlToBe(base + "/conta"));
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[normalize-space()='Sair']"))).click();
            wait.until(ExpectedConditions.urlToBe(base + "/entrar"));

            driver.get(planner);
            wait.until(ExpectedConditions.urlToBe(base + "/entrar"));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email")));
        } finally {
            driver.quit();
        }
    }
}
