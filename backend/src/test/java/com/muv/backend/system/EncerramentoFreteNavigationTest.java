package com.muv.backend.system;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
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
class EncerramentoFreteNavigationTest extends NavegacaoExecucaoFixture {
    private static final String SCHEMA = "rf010_browser_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void esquemaIsolado(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
    }

    @Test
    void encerraEncomendaReenviaAposPerdaDaRespostaERecarrega() throws Exception {
        WebDriver driver = entrar("atual");
        try {
            driver.manage().window().setSize(new Dimension(1360, 1000));
            abrirEncerramento(driver, 910062);
            var main = driver.findElement(By.tagName("main"));
            assertThat(main.getText()).contains("Motorista Atual", "Responsável pelo registro",
                    "Nenhuma entrega será registrada", "Situação da encomenda");
            preencher(driver, "Via segue bloqueada");
            driver.findElement(By.name("situacaoEncomenda")).sendKeys("Sob guarda do prestador");
            clicar(driver, "Encerrar sem conclusão");
            assertThat(driver.findElement(By.name("confirmou")).isSelected()).isFalse();
            conferirEstado(910062, "INTERROMPIDO", 0);
            driver.findElement(By.name("confirmou")).click();
            captura(driver, "encerramento-desktop.png");

            simularPerdaDaResposta(driver);
            clicar(driver, "Encerrar sem conclusão");
            esperarTexto(driver, "A confirmação ficou sem resposta");
            conferirEstado(910062, "ENCERRADO_SEM_CONCLUSAO", 1);
            clicar(driver, "Tentar novamente");
            esperarTexto(driver, "Encerramento sem conclusão registrado.");
            assertThat((List<?>) js(driver).executeScript("return window.enviosEncerramento")).hasSize(2);
            assertThat(js(driver).executeScript("""
                    return window.enviosEncerramento[0] === window.enviosEncerramento[1] &&
                           window.resultadosEncerramento.length === 2 &&
                           window.resultadosEncerramento[0].eventoId === window.resultadosEncerramento[1].eventoId
                    """)).isEqualTo(true);

            driver.navigate().refresh();
            verificarResultado(driver, 910062);
            assertThat(driver.findElement(By.tagName("main")).getText())
                    .contains("Via segue bloqueada").doesNotContain("Sob guarda do prestador");
            assertThat(db.sql("""
                    SELECT situacao_encomenda FROM rf010_recibo_encerramento WHERE frete_id = 910062
                    """).query(String.class).single()).isEqualTo("Sob guarda do prestador");
            captura(driver, "encerramento-apos-recarregar.png");
        } finally {
            driver.quit();
        }
    }

    @Test
    void encerraFreteDePessoasPeloPrestadorSemCampoDeEncomenda() throws Exception {
        prepararInterrupcao(910061);
        WebDriver driver = entrar("empresa");
        try {
            driver.manage().window().setSize(new Dimension(390, 975));
            abrirEncerramento(driver, 910061);
            assertThat(driver.findElements(By.name("situacaoEncomenda"))).isEmpty();
            assertThat(driver.findElement(By.tagName("main")).getText()).contains("Empresa Teste");
            assertThat(js(driver).executeScript("""
                    const form = document.querySelector('main form').getBoundingClientRect();
                    const painel = document.querySelector('main aside').getBoundingClientRect();
                    return painel.top >= form.bottom;
                    """)).isEqualTo(true);
            preencher(driver, "O transporte não pode prosseguir");
            driver.findElement(By.name("confirmou")).click();
            captura(driver, "encerramento-tela-estreita.png");
            clicar(driver, "Encerrar sem conclusão");
            esperarTexto(driver, "Encerramento sem conclusão registrado.");
            driver.navigate().refresh();
            verificarResultado(driver, 910061);
            assertThat(db.sql("""
                    SELECT r.situacao_encomenda IS NULL AND h.usuario_autor_id = 910002
                    FROM rf010_recibo_encerramento r JOIN historico_execucao_frete h ON h.id = r.evento_id
                    WHERE r.frete_id = 910061
                    """).query(Boolean.class).single()).isTrue();
        } finally {
            driver.quit();
        }
    }

    @Test
    void voltarDaRetomadaEDoEncerramentoPreservaAInterrupcaoSemPost() {
        long interrupcaoId = prepararInterrupcao(910063);
        long eventos = contarEventos(910063);
        WebDriver driver = entrar("atual");
        try {
            driver.get(base + "/fretes/910063/execucao");
            esperarTexto(driver, "FR-910063 · Interrompido");
            simularPerdaDaResposta(driver);
            clicar(driver, "Retomar execução");
            preencher(driver, "Rascunho de retomada");
            clicar(driver, "Voltar ao acompanhamento");
            esperarTexto(driver, "FR-910063 · Interrompido");
            clicar(driver, "Encerrar sem conclusão");
            preencher(driver, "Rascunho de encerramento");
            clicar(driver, "Voltar e manter interrompido");
            esperarTexto(driver, "FR-910063 · Interrompido");
            assertThat(js(driver).executeScript("return window.postsExecucao")).isEqualTo(0L);
            conferirEstado(910063, "INTERROMPIDO", 0);
            assertThat(contarEventos(910063)).isEqualTo(eventos);
            assertThat(db.sql("""
                    SELECT id FROM historico_execucao_frete WHERE frete_id = 910063 AND tipo_evento = 'INTERRUPCAO'
                    """).query(Long.class).single()).isEqualTo(interrupcaoId);
        } finally {
            driver.quit();
        }
    }

    private long prepararInterrupcao(long frete) {
        db.sql("UPDATE frete SET status = 'INTERROMPIDO' WHERE id = :frete").param("frete", frete).update();
        return db.sql("""
                INSERT INTO historico_execucao_frete (frete_id, usuario_autor_id, tipo_evento, justificativa)
                VALUES (:frete, 910003, 'INTERRUPCAO', 'Via bloqueada') RETURNING id
                """).param("frete", frete).query(Long.class).single();
    }

    private void verificarResultado(WebDriver driver, long frete) {
        esperarTexto(driver, "FR-" + frete + " · Encerrado sem conclusão");
        assertThat(driver.findElement(By.tagName("main")).getText())
                .doesNotContain("Entrega concluída", "Frete cancelado");
        assertThat(driver.findElements(By.xpath("//button[normalize-space()='Retomar execução']"))).isEmpty();
        conferirEstado(frete, "ENCERRADO_SEM_CONCLUSAO", 1);
        assertThat(db.sql("""
                SELECT data_hora_conclusao IS NULL AND responsavel_registro_conclusao_id IS NULL
                      AND nome_destinatario IS NULL AND cpf_destinatario IS NULL
                FROM frete WHERE id = :frete
                """).param("frete", frete).query(Boolean.class).single()).isTrue();
        assertThat(db.sql("""
                SELECT count(*) FROM historico_execucao_frete WHERE frete_id = :frete
                AND tipo_evento IN ('CONCLUSAO', 'CANCELAMENTO')
                """).param("frete", frete).query(Long.class).single()).isZero();
    }

    private void conferirEstado(long frete, String estado, long encerramentos) {
        assertThat(db.sql("SELECT status FROM frete WHERE id = :frete").param("frete", frete)
                .query(String.class).single()).isEqualTo(estado);
        assertThat(db.sql("""
                SELECT count(*) FROM historico_execucao_frete WHERE frete_id = :frete
                  AND tipo_evento = 'ENCERRAMENTO_SEM_CONCLUSAO'
                """).param("frete", frete).query(Long.class).single()).isEqualTo(encerramentos);
        assertThat(db.sql("SELECT count(*) FROM rf010_recibo_encerramento WHERE frete_id = :frete")
                .param("frete", frete).query(Long.class).single()).isEqualTo(encerramentos);
        assertThat(db.sql("SELECT count(*) FROM notificacao WHERE frete_id = :frete")
                .param("frete", frete).query(Long.class).single()).isZero();
    }

    private long contarEventos(long frete) {
        return db.sql("SELECT count(*) FROM historico_execucao_frete WHERE frete_id = :frete")
                .param("frete", frete).query(Long.class).single();
    }

    private void abrirEncerramento(WebDriver driver, long frete) {
        driver.get(base + "/fretes/" + frete + "/execucao");
        clicar(driver, "Encerrar sem conclusão");
        esperarTexto(driver, "Encerre sem marcar como concluído");
    }

    private void simularPerdaDaResposta(WebDriver driver) {
        js(driver).executeScript("""
                const original = window.fetch.bind(window);
                window.postsExecucao = 0;
                window.enviosEncerramento = [];
                window.resultadosEncerramento = [];
                window.fetch = async (url, opcoes) => {
                    const escrita = opcoes?.method === 'POST' && String(url).includes('/execucao/');
                    if (escrita) window.postsExecucao++;
                    const resposta = await original(url, opcoes);
                    if (escrita && String(url).endsWith('/encerramento-sem-conclusao') && resposta.ok) {
                        window.enviosEncerramento.push(opcoes.body);
                        window.resultadosEncerramento.push(await resposta.clone().json());
                        if (window.enviosEncerramento.length === 1) throw new TypeError('Resposta descartada após commit');
                    }
                    return resposta;
                };
                """);
    }

    private void captura(WebDriver driver, String nome) throws Exception {
        Path arquivo = Path.of("target", "rf010-browser", nome);
        Files.createDirectories(arquivo.getParent());
        Files.write(arquivo, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
    }
}
