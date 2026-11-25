package com.muv.backend.system;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.muv.backend.repository.SolicitacoesDisponiveisRepository;
import com.muv.backend.testutil.PrestadorConsultaFixtures;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PrestadorConsultaSystemTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectProvider<WebDriver> drivers;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SolicitacoesDisponiveisRepository solicitacoesDisponiveis;
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
        jdbc.update("UPDATE autorizacao_modalidade SET status = 'APROVADO' WHERE id = 991002");
        jdbc.update("UPDATE regiao_atuacao SET cidade = 'Caucaia' WHERE id = 991002");
        openSearch();
        new Select(wait.until(ExpectedConditions.elementToBeClickable(By.id("service")))).selectByValue("PESSOA");
        selectRegion("CE", "Fortaleza");
        search();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("main > h2"), "2 prestadores encontrados"));
        var cards = driver.findElements(By.cssSelector("article[id^='prestador-']"));
        assertTrue(cards.get(0).getDomAttribute("id").equals("prestador-991001"));
        assertTrue(cards.get(1).getText().contains("Atende município próximo · Caucaia - CE"));
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
        new Select(wait.until(ExpectedConditions.elementToBeClickable(By.id("service")))).selectByValue("ENCOMENDA");
        selectRegion("PE", "Recife");
        new Select(driver.findElement(By.id("provider-type"))).selectByValue("EMPRESA");
        search();

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("main > h2"), "1 prestador encontrado"));
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
        driver.manage().window().setSize(new Dimension(390, 844));
        openSearch();
        new Select(wait.until(ExpectedConditions.elementToBeClickable(By.id("service")))).selectByValue("PESSOA");
        selectRegion("RN", "Natal");
        search();

        By noResults = By.xpath("//*[normalize-space()='Nenhum prestador encontrado na cidade selecionada ou nas proximidades para os filtros informados.']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(noResults));
        assertTrue(Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                "return document.documentElement.scrollWidth <= window.innerWidth")));
        saveScreenshot("rf005-sem-resultados-natal-rn.png");
    }

    @Test
    void sendsPassengerRequestToTheProviderChosenInRegionalSearch() {
        long cliente = 991099;
        try {
            entrarCliente(cliente);
            openSearch();
            new Select(wait.until(ExpectedConditions.elementToBeClickable(By.id("service")))).selectByValue("PESSOA");
            selectRegion("CE", "Fortaleza");
            search();
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("#prestador-991001 a[href='/cliente/planejar-viagem']"))).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("embarque"))).sendKeys("Embarque");
            driver.findElement(By.name("destino")).sendKeys("Destino");
            ((JavascriptExecutor) driver).executeScript("""
                    const setValue = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
                    for (const [name, value] of [['dataIda', arguments[0]], ['horaIda', '09:00']]) {
                      const input = document.querySelector('[name=' + name + ']');
                      setValue.call(input, value);
                      input.dispatchEvent(new Event('input', {bubbles: true}));
                      input.dispatchEvent(new Event('change', {bubbles: true}));
                    }
                    """, LocalDate.now().plusDays(5).toString());
            clickButton("Consultar prestadores");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//summary[.='Ver detalhes']"))).click();
            clickButton("Revisar solicitação para Autônomo Horizonte Teste");
            preencherEnderecos("Fortaleza", "CE");
            clickButton("Enviar solicitação direcionada");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.partialLinkText("Acompanhar em Meus fretes")));
            Long destinatario = jdbc.queryForObject("""
                    SELECT id_prestador FROM solicitacao_frete
                    WHERE id_passageiro = ? AND modalidade = 'PESSOA' AND status = 'ABERTA'
                    """, Long.class, cliente);
            assertTrue(Long.valueOf(991001).equals(destinatario));
            long solicitacao = jdbc.queryForObject(
                    "SELECT id FROM solicitacao_frete WHERE id_passageiro = ?", Long.class, cliente);
            assertTrue(solicitacoesDisponiveis.listar(991001, "PESSOA", null, null)
                    .stream().anyMatch(item -> item.id() == solicitacao && item.direcionada()));
            assertTrue(solicitacoesDisponiveis.listar(991002, "PESSOA", null, null)
                    .stream().noneMatch(item -> item.id() == solicitacao));
        } finally {
            limparCliente(cliente);
        }
    }

    @Test
    void sendsParcelRequestToTheSelectedProvider() {
        long cliente = 991099;
        try {
            entrarCliente(cliente);
            openSearch();
            new Select(wait.until(ExpectedConditions.elementToBeClickable(By.id("service")))).selectByValue("ENCOMENDA");
            selectRegion("PE", "Recife");
            search();
            clickButton("Solicitar a este prestador");
            wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("solicitar-encomenda"),
                    "Cooperativa Rota Azul Fictícia"));
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelector('[name=partida]').value = arguments[0]",
                    LocalDate.now().plusDays(5) + "T09:00");
            Map.of("descricaoVolumes", "2 caixas", "pesoKg", "25", "alturaCm", "50", "larguraCm", "40",
                    "profundidadeCm", "30", "coleta.nome", "Coleta Teste", "coleta.telefone", "85999999999",
                    "entrega.nome", "Entrega Teste", "entrega.telefone", "85999999999")
                    .forEach((name, value) -> driver.findElement(By.name(name)).sendKeys(value));
            preencherEnderecos("Recife", "PE");
            clickButton("Enviar solicitação direcionada");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.partialLinkText("Acompanhar em Meus fretes")));
            long id = jdbc.queryForObject("""
                    SELECT sf.id FROM solicitacao_frete sf JOIN solicitacao_encomenda se ON se.id = sf.id
                    WHERE sf.id_passageiro = ? AND sf.id_prestador = 991003 AND sf.modalidade = 'ENCOMENDA'
                      AND sf.status = 'ABERTA' AND se.descricao_volumes = '2 caixas'
                    """, Long.class, cliente);
            assertTrue(solicitacoesDisponiveis.listar(991003, "ENCOMENDA", null, null)
                    .stream().anyMatch(item -> item.id() == id && item.direcionada()));
            assertTrue(solicitacoesDisponiveis.listar(991001, "ENCOMENDA", null, null)
                    .stream().noneMatch(item -> item.id() == id));
        } finally {
            limparCliente(cliente);
        }
    }

    private void entrarCliente(long cliente) {
        jdbc.update("INSERT INTO usuario (id, nome, telefone, email, senha) VALUES (?, ?, ?, ?, ?)",
                cliente, "Cliente RF005", "85990001099", "cliente-rf005@example.test",
                passwordEncoder.encode("Teste_RF005#"));
        jdbc.update("INSERT INTO passageiro (id, cpf) VALUES (?, ?)", cliente, "99109900000");
        driver.get(baseUrl + "/entrar");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys("cliente-rf005@example.test");
        driver.findElement(By.name("senha")).sendKeys("Teste_RF005#");
        driver.findElement(By.cssSelector("form button:not([type='button'])")).click();
        wait.until(ExpectedConditions.urlContains("/cliente/planejar-viagem"));
    }

    private void preencherEnderecos(String cidade, String estado) {
        for (int point = 0; point < 2; point++) {
            Map<String, String> address = Map.of("logradouro", "Rua Teste", "numero", "10",
                    "bairro", "Centro", "cidade", cidade, "estado", estado, "cep", "60000000");
            for (var field : address.entrySet()) {
                driver.findElement(By.name(point + "." + field.getKey())).sendKeys(field.getValue());
            }
        }
    }

    private void limparCliente(long cliente) {
        var addresses = jdbc.queryForList("""
                SELECT endereco_origem_id FROM solicitacao_frete WHERE id_passageiro = ?
                UNION SELECT endereco_destino_id FROM solicitacao_frete WHERE id_passageiro = ?
                """, Long.class, cliente, cliente);
        jdbc.update("DELETE FROM notificacao WHERE solicitacao_id IN (SELECT id FROM solicitacao_frete WHERE id_passageiro = ?)", cliente);
        jdbc.update("DELETE FROM solicitacao_pessoa WHERE id IN (SELECT id FROM solicitacao_frete WHERE id_passageiro = ?)", cliente);
        jdbc.update("DELETE FROM solicitacao_encomenda WHERE id IN (SELECT id FROM solicitacao_frete WHERE id_passageiro = ?)", cliente);
        jdbc.update("DELETE FROM solicitacao_frete WHERE id_passageiro = ?", cliente);
        for (long address : addresses) jdbc.update("DELETE FROM endereco WHERE id = ?", address);
        jdbc.update("DELETE FROM passageiro WHERE id = ?", cliente);
        jdbc.update("DELETE FROM usuario WHERE id = ?", cliente);
    }

    private void openSearch() {
        driver.get(baseUrl.replaceAll("/+$", "") + "/cliente/prestadores");
    }

    private void selectRegion(String state, String city) {
        new Select(wait.until(ExpectedConditions.elementToBeClickable(By.id("region-state")))).selectByValue(state);
        new Select(wait.until(ExpectedConditions.elementToBeClickable(By.id("region")))).selectByVisibleText(city);
    }

    private void search() {
        clickButton("Pesquisar");
    }

    private void clickButton(String label) {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='" + label + "']"))).click();
    }

    private void saveScreenshot(String fileName) throws IOException {
        Path destination = Path.of(evidenceDirectory);
        Files.createDirectories(destination);
        Path screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
        Files.copy(screenshot, destination.resolve(fileName),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
