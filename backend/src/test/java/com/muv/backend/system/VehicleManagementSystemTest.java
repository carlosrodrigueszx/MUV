package com.muv.backend.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import com.muv.backend.config.SeleniumConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class VehicleManagementSystemTest {
    @Autowired private ObjectProvider<WebDriver> drivers;
    @Autowired private JdbcTemplate jdbc;
    @Value("${selenium.base-url:http://localhost:5173}") private String baseUrl;
    @Value("${rf004.evidence-dir:../docs/testes/evidencias/RF004}") private String evidenceDir;

    @Test
    void providerCreatesEditsAndPersistsVehicleThroughInterface() throws IOException {
        String email = System.getenv("MUV_E2E_PROVIDER_EMAIL");
        String password = System.getenv("MUV_E2E_PROVIDER_PASSWORD");
        assumeTrue(email != null && !email.isBlank() && password != null && !password.isBlank(),
                "Configure MUV_E2E_PROVIDER_EMAIL e MUV_E2E_PROVIDER_PASSWORD para executar Selenium.");

        String plate = "ZZZ" + (System.currentTimeMillis() % 10) + "A"
                + String.format("%02d", (System.currentTimeMillis() / 10) % 100);
        String base = baseUrl.replaceAll("/+$", "");
        WebDriver driver = drivers.getObject();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        Long vehicleId = null;
        boolean success = false;
        try {
            driver.get(base + "/entrar");
            wait.until(visibilityOfElementLocated(By.name("email"))).sendKeys(email);
            driver.findElement(By.name("senha")).sendKeys(password);
            driver.findElement(By.cssSelector("form button:not([type='button'])")).click();
            wait.until(org.openqa.selenium.support.ui.ExpectedConditions.urlToBe(
                    base + "/cliente/planejar-viagem"));
            driver.get(base + "/prestador/veiculos");
            wait.until(visibilityOfElementLocated(By.xpath("//h1[normalize-space()='Gestão de veículos']")));

            driver.findElement(By.xpath("//button[contains(., 'Cadastrar veículo')]")).click();
            wait.until(visibilityOfElementLocated(By.id("vehicle-brand"))).sendKeys("RF004 E2E");
            driver.findElement(By.id("vehicle-model")).sendKeys("Teste Selenium");
            driver.findElement(By.id("vehicle-year")).clear();
            driver.findElement(By.id("vehicle-year")).sendKeys("2025");
            driver.findElement(By.id("vehicle-plate")).sendKeys(plate);
            driver.findElement(By.id("vehicle-color")).sendKeys("Azul");
            driver.findElement(By.id("vehicle-capacity")).clear();
            driver.findElement(By.id("vehicle-capacity")).sendKeys("15");
            driver.findElement(By.id("vehicle-description")).sendKeys("Registro sistêmico RF004");
            driver.findElement(By.xpath("//button[normalize-space()='Cadastrar veículo']")).click();

            wait.until(visibilityOfElementLocated(By.xpath("//*[contains(normalize-space(), '" + plate + "')]")));
            vehicleId = jdbc.queryForObject("""
                    SELECT id FROM veiculo WHERE placa = ?
                      AND id_prestador = (SELECT id FROM usuario WHERE lower(email) = lower(?))
                    """, Long.class, plate, email);
            assertTrue(vehicleId != null, "A criação via tela precisa persistir no banco de teste.");

            driver.findElement(By.xpath("//article[contains(., '" + plate + "')]//button[contains(., 'Editar veículo')]")).click();
            wait.until(visibilityOfElementLocated(By.id("vehicle-color")));
            var color = driver.findElement(By.id("vehicle-color"));
            color.clear();
            color.sendKeys("Verde");
            driver.findElement(By.xpath("//button[normalize-space()='Salvar alterações']")).click();
            wait.until(visibilityOfElementLocated(By.xpath("//article[contains(., 'Verde') and contains(., '" + plate + "')]")));

            String persistedColor = jdbc.queryForObject("SELECT cor FROM veiculo WHERE id = ?",
                    String.class, vehicleId);
            assertEquals("Verde", persistedColor,
                    "A edição enviada pela interface precisa persistir no banco de teste.");
            success = true;
        } catch (RuntimeException | AssertionError failure) {
            saveScreenshot(driver);
            throw failure;
        } finally {
            if (vehicleId != null) {
                jdbc.update("DELETE FROM veiculo WHERE id = ?", vehicleId);
            }
            driver.quit();
            if (success) {
                Files.createDirectories(Path.of(evidenceDir));
                Files.writeString(Path.of(evidenceDir, "RF004-selenium.txt"),
                        "Cadastro, edição, leitura na interface e persistência no banco: aprovado.\nPlaca: " + plate);
            }
        }
    }

    private void saveScreenshot(WebDriver driver) throws IOException {
        Files.createDirectories(Path.of(evidenceDir));
        Files.write(Path.of(evidenceDir, "falha-gestao-veiculos.png"),
                ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }
}
