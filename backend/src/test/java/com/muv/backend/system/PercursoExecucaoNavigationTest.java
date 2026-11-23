package com.muv.backend.system;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;

// Usa o WebDriver do projeto e o proxy /api do Vite para localhost:8080.
@EnabledIfSystemProperty(named = "muv.rf010.e2e", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "selenium.headless=true")
@ActiveProfiles("rf010-test")
@Sql(scripts = "/db/rf010-consulta.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(statements = "ALTER SEQUENCE historico_execucao_frete_id_seq RESTART WITH 910075",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class PercursoExecucaoNavigationTest extends NavegacaoExecucaoFixture {
    private static final String SCHEMA = "rf010_browser_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void esquemaIsolado(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
    }

    @ParameterizedTest
    @CsvSource({"911001,true,true", "911002,true,false", "911003,false,true", "911004,false,false"})
    void contratacaoAteDesfecho(long solicitacao, boolean pessoas, boolean retomar) throws Exception {
        propostaAberta(solicitacao, pessoas);
        WebDriver cliente = entrar("cliente");
        try {
            cliente.get(base + "/cliente/solicitacoes/" + solicitacao + "/propostas");
            clicar(cliente, "Ver proposta");
            cliente.findElement(By.cssSelector("input[type=checkbox]")).click();
            clicar(cliente, "Confirmar contratação de Empresa Teste");
            espera(cliente).until(ExpectedConditions.urlContains("/cliente/fretes/"));
            long id = Long.parseLong(cliente.getCurrentUrl().replaceAll(".*/", ""));
            String rota = base + "/fretes/" + id + "/execucao";
            WebDriver empresa = entrar("empresa");
            try {
                empresa.get(base + "/prestador/fretes/" + id + "/designacao");
                clicarOpcao(empresa, 0, "910011");
                clicarOpcao(empresa, 1, pessoas ? "910021" : "910022");
                clicar(empresa, "Salvar designação");
                esperarTexto(empresa, "Designação salva");
                WebDriver motorista = entrar("atual");
                try {
                    motorista.get(rota);
                    clicar(motorista, "Iniciar execução");
                    clicar(motorista, "Confirmar início");
                    esperarTexto(motorista, "Execução iniciada.");
                    assertThat(motorista.findElements(By.cssSelector("nav a[href='/prestador/veiculos'],nav a[href='/prestador/motoristas']"))).isEmpty();
                    empresa.get(rota);
                    clicar(empresa, "Registrar interrupção");
                    new Select(empresa.findElement(By.name("tipo"))).selectByValue("OCORRENCIA");
                    esperarTexto(empresa, "O frete permanece em execução.");
                    preencher(empresa, "Aviso do prestador");
                    clicar(empresa, "Registrar ocorrência");
                    esperarTexto(empresa, "Ocorrência registrada.");
                    clicar(motorista, "Registrar interrupção");
                    preencher(motorista, "Via bloqueada");
                    clicar(motorista, "Registrar e interromper execução");
                    esperarTexto(motorista, "Interrupção registrada.");
                    empresa.navigate().refresh();
                    clicar(empresa, retomar ? "Retomar execução" : "Encerrar sem conclusão");
                    if (retomar) {
                        esperarTexto(empresa, "Via bloqueada");
                        esperarTexto(empresa, "Motorista Atual");
                    } else {
                        esperarTexto(empresa, "Esta ação não é uma conclusão");
                    }
                    preencher(empresa, retomar ? "Via liberada" : "Não foi possível prosseguir");
                    empresa.findElement(By.name("confirmou")).click();
                    clicar(empresa, retomar ? "Confirmar retomada" : "Encerrar sem conclusão");
                    esperarTexto(empresa, retomar ? "Retomada registrada." : "Encerramento sem conclusão registrado.");
                    assertThat(empresa.switchTo().activeElement().getDomAttribute("role")).isEqualTo("status");
                    empresa.navigate().refresh();
                    esperarTexto(empresa, "FR-" + id + " · " + (retomar ? "Em execução" : "Encerrado sem conclusão"));
                    assertThat(db.sql("""
                            SELECT tipo_evento || '/' || usuario_autor_id FROM historico_execucao_frete
                            WHERE frete_id = :id ORDER BY id
                            """).param("id", id).query(String.class).list()).containsExactly(
                            "DESIGNACAO/910002", "INICIO/910003", "OCORRENCIA/910002", "INTERRUPCAO/910003",
                            (retomar ? "RETOMADA" : "ENCERRAMENTO_SEM_CONCLUSAO") + "/910002");
                    String timeline = (String) js(cliente).executeAsyncScript("""
                            const [id, done] = arguments;
                            fetch('/api/historico-fretes?perfil=CLIENTE').then(r => r.json())
                              .then(rows => done(JSON.stringify(rows.find(f => f.id === 'FR-' + id)?.timeline) || ''))
                              .catch(e => done(String(e)));
                            """, id);
                    assertThat(timeline).contains("Execução iniciada", "Ocorrência registrada", "Interrupção registrada",
                            retomar ? "Execução retomada" : "Encerrado sem conclusão");
                } finally { motorista.quit(); }
            } finally { empresa.quit(); }
        } finally { cliente.quit(); }
    }

    private void clicarOpcao(WebDriver driver, int indice, String valor) {
        var selects = espera(driver).until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("main select")));
        new Select(selects.get(indice)).selectByValue(valor);
    }

    private void propostaAberta(long id, boolean pessoas) {
        db.sql("""
                INSERT INTO solicitacao_frete(id, modalidade, data_hora_pretendida, status, id_passageiro,
                    endereco_origem_id, endereco_destino_id, valor_cliente, condicoes_cliente)
                VALUES (:id, :modalidade, CURRENT_TIMESTAMP + interval '1 day', 'ABERTA', 910001, 910031, 910032, 100, 'Percurso RF010')
                """).param("id", id).param("modalidade", pessoas ? "PESSOA" : "ENCOMENDA").update();
        if (pessoas) {
            db.sql("INSERT INTO solicitacao_pessoa(id, quantidade_pessoas, possui_bagagem) VALUES (:id, 4, false)")
                    .param("id", id).update();
        } else {
            db.sql("""
                    INSERT INTO solicitacao_encomenda(id,descricao_volumes,peso_estimado_kg,altura_cm,largura_cm,
                        profundidade_cm,nome_contato_coleta,telefone_contato_coleta,nome_contato_entrega,telefone_contato_entrega)
                    VALUES (:id,'Caixa',25,50,40,30,'Coleta','85911111001','Entrega','85911111002')
                    """).param("id", id).update();
        }
        db.sql("""
                INSERT INTO proposta(status,valor,condicoes,id_solicitacao,id_prestador)
                VALUES ('ENVIADA',100,'Condições do percurso',:id,910002)
                """).param("id", id).update();
    }

    @Test
    void ciclosNavegacaoTecladoERecuperacao() throws Exception {
        WebDriver driver = entrar("atual");
        try {
            String rota = base + "/fretes/910063/execucao";
            driver.get(rota);
            clicar(driver, "Registrar ocorrência");
            preencher(driver, "Texto do ciclo anterior");
            String ocorrencia = driver.getWindowHandle();
            driver.switchTo().newWindow(org.openqa.selenium.WindowType.TAB);
            driver.get(rota);
            clicar(driver, "Registrar interrupção");
            preencher(driver, "Interrupção antiga");
            String interrupcao = driver.getWindowHandle();
            driver.switchTo().newWindow(org.openqa.selenium.WindowType.TAB);
            driver.get(rota);
            clicar(driver, "Registrar interrupção");
            tecladoECaptura(driver, "interrupcao", List.of("tipo","data","horario","texto","submit","voltar"));
            preencher(driver, "Interrupção de teste");
            clicar(driver, "Registrar e interromper execução");
            esperarTexto(driver, "Interrupção registrada.");
            clicar(driver, "Encerrar sem conclusão");
            tecladoECaptura(driver, "encerramento", List.of("data","horario","texto","situacaoEncomenda","confirmou","submit","voltar"));
            clicar(driver, "Voltar e manter interrompido");
            clicar(driver, "Retomar execução");
            esperarTexto(driver, "Interrupção de teste");
            esperarTexto(driver, "Motorista Atual");
            esperarTexto(driver, "Recursos confirmados");
            tecladoECaptura(driver, "retomada", List.of("data","horario","texto","confirmou","submit","voltar"));
            preencher(driver, "Via liberada");
            driver.findElement(By.name("confirmou")).click();
            js(driver).executeScript("""
                    const original = window.fetch.bind(window);
                    window.fetch = async (url, opts) => {
                        if (String(url).endsWith('/execucao')) {
                            window.fetch = original;
                            return new Response('', {status:503});
                        }
                        return original(url, opts);
                    };
                    """);
            clicar(driver, "Confirmar retomada");
            esperarTexto(driver, "Retomada registrada.");
            esperarTexto(driver, "Não foi possível consultar o frete");
            assertThat(driver.switchTo().activeElement().getDomAttribute("role")).isEqualTo("alert");
            clicar(driver, "Tentar novamente");
            esperarTexto(driver, "FR-910063 · Em execução");
            assertThat(driver.switchTo().activeElement().getText()).isEqualTo("Retomada registrada.");
            for (String aba : List.of(interrupcao, ocorrencia)) {
                driver.switchTo().window(aba);
                boolean registro = aba.equals(ocorrencia);
                clicar(driver, registro ? "Registrar ocorrência" : "Registrar e interromper execução");
                esperarTexto(driver, "Atualizar acompanhamento");
                assertThat(driver.switchTo().activeElement().getDomAttribute("role")).isEqualTo("alert");
                clicar(driver, "Atualizar acompanhamento");
                clicar(driver, registro ? "Registrar ocorrência" : "Registrar interrupção");
                assertThat(driver.findElement(By.name("texto")).getDomProperty("value"))
                        .isEqualTo(registro ? "Texto do ciclo anterior" : "Interrupção antiga");
            }
            assertThat(db.sql("SELECT tipo_evento FROM historico_execucao_frete WHERE frete_id=910063 ORDER BY id")
                    .query(String.class).list()).containsExactly("INICIO","OCORRENCIA","INTERRUPCAO","RETOMADA");
            driver.get(base + "/fretes/910061/execucao");
            esperarTexto(driver, "FR-910061 · Pronto para início");
            assertThat(driver.findElement(By.tagName("main")).getText()).doesNotContain("Texto do ciclo anterior", "Via liberada");
            assertThat(driver.findElements(By.name("texto"))).isEmpty();
        } finally { driver.quit(); }
    }

    private void tecladoECaptura(WebDriver driver, String tela, List<String> ordem) throws Exception {
        espera(driver).until(d -> d.switchTo().activeElement().getTagName().equals("h1"));
        var vistos = new ArrayList<String>();
        for (int i = 0; i < 30 && !vistos.contains("voltar"); i++) {
            new org.openqa.selenium.interactions.Actions(driver).sendKeys(Keys.TAB).perform();
            var ativo = driver.switchTo().activeElement();
            String campo = ativo.getDomAttribute("name");
            if (campo == null && ativo.getTagName().equals("button")) {
                campo = "submit".equals(ativo.getDomAttribute("type")) ? "submit" : "voltar";
            }
            if (campo != null && (vistos.isEmpty() || !campo.equals(vistos.getLast()))) vistos.add(campo);
        }
        assertThat(vistos).containsExactlyElementsOf(ordem);
        new org.openqa.selenium.interactions.Actions(driver).keyDown(Keys.SHIFT).sendKeys(Keys.TAB)
                .keyUp(Keys.SHIFT).sendKeys(Keys.ENTER).perform();
        assertThat(driver.switchTo().activeElement().getDomAttribute("name")).isEqualTo("data");
        for (int largura : new int[]{1360, 390}) {
            driver.manage().window().setSize(new Dimension(largura, 1000));
            if (largura == 390) assertThat(js(driver).executeScript("""
                    return document.querySelector('main aside').getBoundingClientRect().top >=
                           document.querySelector('main form').getBoundingClientRect().bottom;
                    """)).isEqualTo(true);
            Path destino = Path.of("target", "rf010-browser", tela + "-" + largura + ".png");
            Files.createDirectories(destino.getParent());
            Files.write(destino, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        }
        driver.manage().window().setSize(new Dimension(1360, 1000));
    }
}
