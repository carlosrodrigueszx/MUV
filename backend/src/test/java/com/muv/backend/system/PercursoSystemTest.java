package com.muv.backend.system;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.support.ui.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.servlet.config.annotation.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.web.resources.static-locations=file:../frontend/dist/")
@ActiveProfiles("test")
@Import(PercursoSystemTest.Spa.class)
@Sql("/db/rf007-solicitacoes.sql")
@Sql(scripts="/db/rf007-clean.sql", executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PercursoSystemTest {
    @TestConfiguration
    static class Spa implements WebMvcConfigurer {
        @Override public void addViewControllers(ViewControllerRegistry registry) {
            for (String path : List.of("/entrar", "/prestador/solicitacoes/{id}/avaliacao")) {
                registry.addViewController(path).setViewName("forward:/index.html");
            }
        }
    }

    static final HttpServer provedor = criarProvedor();
    static volatile boolean semRota;
    @LocalServerPort int porta;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectProvider<WebDriver> drivers;
    WebDriver driver;
    WebDriverWait wait;

    private static HttpServer criarProvedor() {
        try {
            var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.createContext("/api/", troca -> {
                String consulta = URLDecoder.decode(troca.getRequestURI().getQuery(), StandardCharsets.UTF_8);
                double lon = -39;
                if (consulta.contains("Rua do Sol")) lon = -38;
                if (consulta.contains("Parada Um")) lon = -38.3;
                if (consulta.contains("Parada Dois")) lon = -38.7;
                String json = """
                        {"features":[{"properties":{"countrycode":"BR","type":"house"},
                        "geometry":{"coordinates":[%s,-4]}}]}
                        """.formatted(lon);
                responder(troca, json);
            });
            servidor.createContext("/route/v1/driving/", troca -> {
                String[] pontos = troca.getRequestURI().getPath().replace("/route/v1/driving/", "").split(";");
                String geometria = java.util.Arrays.stream(pontos).map(p -> "[" + p + "]")
                        .collect(java.util.stream.Collectors.joining(","));
                int trechos = pontos.length - 1;
                String pernas = trechos == 1 ? "{\"duration\":1800}" :
                        "{\"duration\":600},{\"duration\":1200},{\"duration\":2400}";
                responder(troca, semRota ? "{\"routes\":[]}" : """
                        {"routes":[{"geometry":{"coordinates":[%s]},
                        "legs":[%s]}]}
                        """.formatted(geometria, pernas));
            });
            servidor.createContext("/tile.png", troca -> {
                byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a6ioAAAAASUVORK5CYII=");
                troca.getResponseHeaders().set("Content-Type", "image/png");
                troca.sendResponseHeaders(200, png.length);
                try (var saida = troca.getResponseBody()) { saida.write(png); }
            });
            servidor.start();
            return servidor;
        } catch (IOException e) { throw new IllegalStateException(e); }
    }

    private static void responder(com.sun.net.httpserver.HttpExchange troca, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json");
        troca.sendResponseHeaders(200, bytes.length);
        try (var saida = troca.getResponseBody()) { saida.write(bytes); }
    }

    static String origemProvedor() { return "http://127.0.0.1:" + provedor.getAddress().getPort(); }

    @DynamicPropertySource static void configurar(DynamicPropertyRegistry registro) {
        registro.add("muv.percurso.photon", () -> origemProvedor() + "/api/");
        registro.add("muv.percurso.osrm", PercursoSystemTest::origemProvedor);
    }

    @AfterAll static void parar() { provedor.stop(0); }

    @BeforeEach void entrar() {
        semRota = false;
        jdbc.update("UPDATE usuario SET senha=? WHERE id=907002", encoder.encode("Teste-rf007"));
        jdbc.update("UPDATE solicitacao_frete SET data_hora_pretendida='2026-11-20 08:00' WHERE id=907031");
        driver = drivers.getObject();
        driver.manage().window().setSize(new Dimension(1440,1114));
        if (driver instanceof ChromiumDriver chromium) {
            chromium.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", Map.of("source", """
                const desc = Object.getOwnPropertyDescriptor(HTMLImageElement.prototype,'src');
                Object.defineProperty(HTMLImageElement.prototype,'src',{...desc,set(url){
                  desc.set.call(this, String(url).startsWith('https://tile.openstreetmap.org/') ? '%s/tile.png' : url);
                }});
                """.formatted(origemProvedor())));
        }
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.get("http://localhost:" + porta + "/entrar");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("email"))).sendKeys("autor907@example.test");
        driver.findElement(By.name("senha")).sendKeys("Teste-rf007");
        driver.findElement(By.cssSelector("form button:not([type='button'])")).click();
        wait.until(d -> !d.getCurrentUrl().endsWith("/entrar"));
    }

    @AfterEach void fechar() { if (driver != null) driver.quit(); }

    void abrir() { driver.get("http://localhost:" + porta + "/prestador/solicitacoes/907031/avaliacao"); }

    void conferir(List<String> marcas, List<String> horarios) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".rf-rota")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".leaflet-tile-loaded")));
        assertEquals(marcas, textos(".rf-pin"));
        assertEquals(horarios, textos(".rf-itinerario time"));
        var js = (JavascriptExecutor) driver;
        var linhas = driver.findElements(By.cssSelector(".rf-itinerario li"));
        for (int i = 0; i < linhas.size(); i++) {
            boolean conector = (boolean) js.executeScript(
                    "return parseFloat(getComputedStyle(arguments[0],'::after').height)>0", linhas.get(i));
            assertEquals(i < linhas.size()-1, conector);
        }
    }

    List<String> textos(String seletor) {
        return driver.findElements(By.cssSelector(seletor)).stream().map(WebElement::getText).toList();
    }

    @Test void direto() {
        abrir();
        conferir(List.of("A","B"), List.of("08:00","08:30"));
    }

    @Test void paradasOrdenadas() throws IOException {
        jdbc.update("""
                INSERT INTO endereco(id,cep,logradouro,numero,bairro,cidade,estado) VALUES
                (907022,'60000-000','Parada Um','10','Centro','Fortaleza','CE'),
                (907023,'60000-000','Parada Dois','20','Centro','Fortaleza','CE')
                """);
        jdbc.update("INSERT INTO solicitacao_parada VALUES (907031,2,907023,'2026-11-20 10:00'),(907031,1,907022,NULL)");
        abrir();
        conferir(List.of("A","1","2","B"), List.of("08:00","08:10","10:00","10:40"));
        var pasta = Path.of("target","rf007-evidencias");
        Files.createDirectories(pasta);
        for (int largura : List.of(1440,390)) {
            driver.manage().window().setSize(new Dimension(largura,1000));
            var mapa = driver.findElement(By.cssSelector(".rf-mapa")).getRect();
            long viewport = ((Number)((JavascriptExecutor)driver).executeScript("return innerWidth")).longValue();
            assertTrue(mapa.x >= 0 && mapa.x + mapa.width <= viewport);
            Files.write(pasta.resolve("percurso-"+largura+".png"), ((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES));
        }
    }

    @Test void falhaPreservaAvaliacao() {
        semRota = true;
        abrir();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".rf-mapa-aviso"),"indisponível"));
        assertEquals(List.of("08:00"), textos(".rf-itinerario time"));
        assertTrue(driver.findElement(By.cssSelector(".rf-itinerario")).getText().contains("Sem previsão"));
        assertTrue(driver.findElement(By.cssSelector(".rf-barra .rf-principal")).isEnabled());
    }
}
