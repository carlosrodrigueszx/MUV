package com.muv.backend.system;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import java.math.BigDecimal;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql("/db/rf007-solicitacoes.sql")
@Sql(scripts="/db/rf007-clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PropostaSystemTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectProvider<WebDriver> drivers;
    @Value("${selenium.base-url}") String base;
    WebDriver driver;
    WebDriverWait wait;

    @BeforeEach void login() {
        jdbc.update("UPDATE usuario SET senha=? WHERE id=907002",encoder.encode("Teste-rf007"));
        driver=drivers.getObject();
        driver.manage().window().setSize(new Dimension(1440,1114));
        wait=new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.get(base+"/entrar");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys("autor907@example.test");
        driver.findElement(By.name("senha")).sendKeys("Teste-rf007");
        driver.findElement(By.cssSelector("form button:not([type='button'])")).click();
        wait.until(d->!d.getCurrentUrl().endsWith("/entrar"));
    }

    @AfterEach void fechar() { if(driver!=null)driver.quit(); }

    void abrir(int id,String preco) {
        driver.get(base+"/prestador/solicitacoes/"+id+"/avaliacao");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".rf-preco"),preco));
        assertTrue(driver.findElement(By.cssSelector(".rf-barra .rf-principal")).getText().contains(preco));
    }

    void clicar(String nome) {
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space(.)='"+nome+"']"))).click();
    }

    @Test void trocaSolicitacaoEEnviaPeloValorDoCliente() {
        abrir(907032,"198,46");
        abrir(907031,"315,78");
        clicar("Fazer contraproposta");
        assertTrue(driver.findElement(By.cssSelector("dialog .rf-oferta")).getText().contains("315,78"));
        clicar("Voltar à avaliação");
        driver.findElement(By.cssSelector(".rf-barra .rf-principal")).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("main [role=status]"),"Proposta enviada"));
        assertEquals(new BigDecimal("315.78"),jdbc.queryForObject("SELECT valor FROM proposta WHERE id_solicitacao=907031 AND id_prestador=907002",BigDecimal.class));
        assertEquals("ABERTA",jdbc.queryForObject("SELECT status FROM solicitacao_frete WHERE id=907031",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE solicitacao_id=907031 AND usuario_destino_id=907001",Integer.class));
    }

    @Test void ajustaEncomendaERecusaComMotivo() {
        abrir(907032,"198,46");
        clicar("Recusar");
        driver.findElement(By.tagName("textarea")).sendKeys("Veículo em manutenção");
        clicar("Confirmar recusa");
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("main [role=status]"),"Veículo em manutenção"));
        abrir(907032,"198,46");
        clicar("Fazer contraproposta");
        clicar("Voltar à avaliação");
        clicar("Fazer contraproposta");
        clicar("Enviar contraproposta");
        assertTrue(driver.findElement(By.cssSelector("dialog[open]")).isDisplayed());
        driver.findElement(By.cssSelector("dialog input")).sendKeys("210,00");
        driver.findElement(By.tagName("textarea")).sendKeys("Inclui ajudante");
        assertTrue(driver.findElement(By.tagName("dialog")).getText().contains("11,54"));
        clicar("Enviar contraproposta");
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("dialog[open]")));
        assertEquals(new BigDecimal("210.00"),jdbc.queryForObject("SELECT valor FROM proposta WHERE id_solicitacao=907032",BigDecimal.class));
        assertTrue(jdbc.queryForObject("SELECT condicoes FROM proposta WHERE id_solicitacao=907032",String.class).contains("Inclui ajudante"));
    }

    @Test void comparaTelaDesktopEMovel() throws Exception {
        abrir(907031,"315,78");
        registrarImagem("avaliacao-desktop");
        clicar("Fazer contraproposta");
        driver.findElement(By.cssSelector("dialog input")).sendKeys("300");
        assertTrue(driver.findElement(By.tagName("dialog")).getText().contains("15,78"));
        registrarImagem("contraproposta-desktop");
        clicar("Voltar à avaliação");
        driver.manage().window().setSize(new Dimension(390,844));
        clicar("Fazer contraproposta");
        var dialogo=driver.findElement(By.tagName("dialog")).getRect();
        long largura=((Number)((JavascriptExecutor)driver).executeScript("return innerWidth")).longValue();
        assertTrue(dialogo.x>=0 && dialogo.x+dialogo.width<=largura);
        registrarImagem("contraproposta-movel");
        clicar("Voltar à avaliação");
        jdbc.update("UPDATE solicitacao_frete SET valor_cliente=NULL WHERE id=907031");
        abrirSemValor();
        assertFalse(driver.findElement(By.cssSelector(".rf-barra .rf-principal")).isEnabled());
    }

    private void abrirSemValor() {
        driver.navigate().refresh();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".rf-preco"),"indisponível"));
    }

    private void registrarImagem(String nome) throws java.io.IOException {
        var pasta=java.nio.file.Path.of("target","rf007-evidencias");
        java.nio.file.Files.createDirectories(pasta);
        java.nio.file.Files.write(pasta.resolve(nome+".png"),((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES));
    }

    @Test void cancelamentoComTelaAbertaBloqueiaEnvio() {
        abrir(907031,"315,78");
        jdbc.update("UPDATE solicitacao_frete SET status='CANCELADA' WHERE id=907031");
        driver.findElement(By.cssSelector(".rf-barra .rf-principal")).click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("[role=alert]"),"encerrada"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM proposta WHERE id_solicitacao=907031",Integer.class));
    }
}
