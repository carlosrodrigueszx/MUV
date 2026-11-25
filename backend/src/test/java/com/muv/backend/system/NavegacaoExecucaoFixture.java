package com.muv.backend.system;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;

@org.springframework.test.annotation.DirtiesContext
abstract class NavegacaoExecucaoFixture {
    @Autowired ObjectProvider<WebDriver> drivers;
    @Autowired JdbcClient db;
    @Autowired PasswordEncoder senhas;
    @Value("${selenium.base-url:http://localhost:5173}") String base;

    void preencher(WebDriver driver, String justificativa) {
        js(driver).executeScript("""
                document.querySelector('[name=data]').value = '2000-01-02';
                document.querySelector('[name=horario]').value = '09:55';
                """);
        driver.findElement(By.name("texto")).sendKeys(justificativa);
    }

    WebDriver entrar(String perfil) {
        String destino = switch (perfil) {
            case "cliente" -> "/cliente/planejar-viagem";
            case "empresa" -> "/prestador";
            default -> "/motorista";
        };
        String email = perfil + "910@example.test";
        db.sql("UPDATE usuario SET senha = :senha WHERE email = :email")
                .param("senha", senhas.encode("Teste_RF010#")).param("email", email).update();
        WebDriver driver = drivers.getObject();
        try {
            driver.get(base + "/entrar");
            espera(driver).until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys(email);
            driver.findElement(By.name("senha")).sendKeys("Teste_RF010#");
            driver.findElement(By.cssSelector("form button:not([type='button'])")).click();
            espera(driver).until(ExpectedConditions.urlToBe(base + destino));
            return driver;
        } catch (RuntimeException erro) {
            driver.quit();
            throw erro;
        }
    }

    void esperarTexto(WebDriver driver, String texto) {
        espera(driver).until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("main"), texto));
    }

    void clicar(WebDriver driver, String texto) {
        espera(driver).until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='" + texto + "']"))).click();
    }

    WebDriverWait espera(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(12));
    }

    JavascriptExecutor js(WebDriver driver) {
        return (JavascriptExecutor) driver;
    }

}
