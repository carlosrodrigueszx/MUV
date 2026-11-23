package com.muv.backend.execucao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("rf010-test")
@Sql(scripts = "/db/rf010-consulta.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class TransicaoFreteIntegrationTest {
    private static final String SCHEMA = "rf010_interrupcao_" + UUID.randomUUID().toString().replace("-", "");
    private static final String ROTA = "/api/fretes/910063/execucao";
    private static final String PESSOAS = "/api/fretes/910061/execucao";
    private static final String INICIO = PESSOAS + "/inicio";
    private static final String CANCELAR = "/api/fretes/910061/cancelamento";
    private static final String COMANDO = ROTA + "/interrupcoes";
    private static final String RETOMADA = ROTA + "/retomadas";
    private static final String ENCERRAMENTO = ROTA + "/encerramento-sem-conclusao";

    @DynamicPropertySource
    static void esquemaIsolado(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcClient db;
    @Autowired PasswordEncoder senhas;
    @Autowired ObjectMapper json;
    @Autowired PlatformTransactionManager transacoes;

    @BeforeEach
    void restaurarFreteEmExecucao() {
        db.sql("DELETE FROM rf010_recibo_ocorrencia WHERE frete_id = 910061").update();
        db.sql("DELETE FROM rf010_recibo_inicio WHERE frete_id = 910061").update();
        db.sql("DELETE FROM notificacao WHERE frete_id = 910061").update();
        db.sql("DELETE FROM historico_execucao_frete WHERE frete_id = 910061").update();
        db.sql("""
                UPDATE frete SET status = 'PRONTO_INICIO', revisao_designacao = 1, motorista_atual_id = 910011,
                    veiculo_atual_id = 910021 WHERE id = 910061
                """).update();
        db.sql("DELETE FROM rf010_recibo_ocorrencia WHERE frete_id = 910063").update();
        db.sql("DELETE FROM rf010_recibo_encerramento WHERE frete_id = 910063").update();
        db.sql("DELETE FROM rf010_recibo_retomada WHERE frete_id = 910063").update();
        db.sql("DELETE FROM rf010_recibo_interrupcao WHERE frete_id = 910063").update();
        db.sql("DELETE FROM historico_execucao_frete WHERE frete_id = 910063 AND id > 910074").update();
        db.sql("ALTER SEQUENCE historico_execucao_frete_id_seq RESTART WITH 910075").update();
        db.sql("""
                UPDATE frete SET status = 'EM_EXECUCAO', motorista_atual_id = 910011, revisao_designacao = 1
                WHERE id = 910063
                """).update();
    }

    private String dadosInicio() {
        return """
                {"chaveOperacao":"%s","estadoEsperado":"PRONTO_INICIO","revisaoDesignacao":1}
                """.formatted(UUID.randomUUID());
    }

    @Test
    void inicioRevalidaSessaoRevisaoMotoristaERecursosAntesDaEscrita() throws Exception {
        var atual = entrar("atual");
        String comando = dadosInicio();
        mvc.perform(get(PESSOAS).session(atual.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.acoesElegiveis[0]").value("INICIO"));
        enviarComando(INICIO, csrf(null), comando, 401);
        mvc.perform(post(INICIO).session(atual.session()).contentType("application/json").content(comando))
                .andExpect(status().isForbidden());
        enviarComando(INICIO, entrar("empresa"), comando, 403);
        enviarComando(INICIO, entrar("colega"), comando, 403);
        db.sql("UPDATE frete SET motorista_atual_id = 910012, revisao_designacao = 2 WHERE id = 910061").update();
        enviarComando(INICIO, atual, comando, 403);
        enviarComando(INICIO, entrar("colega"), comando, 409);
        db.sql("UPDATE frete SET motorista_atual_id = 910011, revisao_designacao = 3 WHERE id = 910061").update();
        enviarComando(INICIO, atual, comando, 409);
        db.sql("UPDATE frete SET veiculo_atual_id = 910022 WHERE id = 910061").update();
        enviarComando(INICIO, atual, comando.replace(":1}", ":3}"), 409);
        conferirInicio("PRONTO_INICIO", 0, 0);
    }

    @Test
    void falhaNoReciboDeInicioMantemFretePronto() throws Exception {
        var motorista = entrar("atual");
        String comando = dadosInicio();
        db.sql("ALTER TABLE rf010_recibo_inicio ADD CONSTRAINT recusar_inicio CHECK (false)").update();
        try {
            assertThatThrownBy(() -> enviarComando(INICIO, motorista, comando, 200))
                    .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class)
                    .hasStackTraceContaining("recusar_inicio");
            conferirInicio("PRONTO_INICIO", 0, 0);
        } finally {
            db.sql("ALTER TABLE rf010_recibo_inicio DROP CONSTRAINT recusar_inicio").update();
        }
        enviarComando(INICIO, motorista, comando, 200);
        conferirInicio("EM_EXECUCAO", 1, 0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ocorrenciaExigeDescricaoDataEHorarioNasDuasModalidades(boolean pessoas) throws Exception {
        var motorista = entrar("atual");
        long ciclo = 910073;
        if (pessoas) {
            var resposta = enviarComando(INICIO, motorista, dadosInicio(), 200);
            ciclo = json.readTree(resposta.getResponse().getContentAsString()).get("eventoId").asLong();
        }
        String rota = (pessoas ? PESSOAS : ROTA) + "/ocorrencias";
        String dados = dados(UUID.randomUUID()).replace("910073", Long.toString(ciclo)).replace("justificativa", "descricao");
        enviarComando(rota, motorista, dados.replace("Via bloqueada", "  "), 400);
        enviarComando(rota, motorista, dados.replace("\"2000-01-02\"", "null"), 400);
        enviarComando(rota, motorista, dados.replace("\"09:15\"", "null"), 400);
        enviarComando(rota, motorista, dados.replace("09:15", "25:15"), 400);
        var resposta = enviarComando(rota, motorista, dados, 200);
        var evento = json.readTree(resposta.getResponse().getContentAsString());
        assertThat(evento.get("estadoAnterior").asString()).isEqualTo("EM_EXECUCAO");
        assertThat(evento.get("estadoResultante").asString()).isEqualTo("EM_EXECUCAO");
        assertThat(db.sql("SELECT count(*) FROM rf010_recibo_ocorrencia WHERE ciclo_execucao_id = :id")
                .param("id", ciclo).query(Integer.class).single()).isOne();
    }

    @Test
    void inicioRecuperaEventoEAbreCicloParaOcorrencia() throws Exception {
        var motorista = entrar("atual");
        String comando = dadosInicio();
        var original = enviarComando(INICIO, motorista, comando, 200).getResponse().getContentAsString();
        assertThat(enviarComando(INICIO, motorista, comando, 200).getResponse().getContentAsString()).isEqualTo(original);
        enviarComando(INICIO, motorista, dadosInicio(), 409);
        var resposta = json.readTree(original);
        assertThat(resposta.get("autorId").asLong()).isEqualTo(910003);
        mvc.perform(get(PESSOAS).session(motorista.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.cicloExecucaoId").value(resposta.get("eventoId").asLong()))
                .andExpect(jsonPath("$.acoesElegiveis[0]").value("OCORRENCIA"));
        conferirInicio("EM_EXECUCAO", 1, 0);
        db.sql("UPDATE frete SET motorista_atual_id = 910012 WHERE id = 910061").update();
        enviarComando(INICIO, motorista, comando, 403);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void inicioECancelamentoDisputamOMesmoFrete(boolean iniciaPrimeiro) throws Exception {
        var motorista = entrar("atual");
        var cliente = entrar("cliente");
        String iniciar = dadosInicio();
        String cancelar = "{\"justificativa\":\"Mudança de planos\"}";
        try (var executor = Executors.newSingleThreadExecutor()) {
            var perdedora = new TransactionTemplate(transacoes).execute(ignorado -> {
                db.sql("SELECT id FROM frete WHERE id = 910061 FOR UPDATE").query(Long.class).single();
                var fila = executor.submit(() -> iniciaPrimeiro
                        ? enviarComando(CANCELAR, cliente, cancelar, 409)
                        : enviarComando(INICIO, motorista, iniciar, 409));
                aguardarDisputaNoPostgres();
                try {
                    if (iniciaPrimeiro) enviarComando(INICIO, motorista, iniciar, 200);
                    else enviarComando(CANCELAR, cliente, cancelar, 204);
                } catch (Exception erro) {
                    throw new AssertionError(erro);
                }
                return fila;
            });
            perdedora.get(10, TimeUnit.SECONDS);
        }
        conferirInicio(iniciaPrimeiro ? "EM_EXECUCAO" : "CANCELADO", iniciaPrimeiro ? 1 : 0, iniciaPrimeiro ? 0 : 1);
    }

    private void conferirInicio(String estado, int inicios, int cancelamentos) {
        assertThat(db.sql("SELECT status FROM frete WHERE id = 910061").query(String.class).single()).isEqualTo(estado);
        assertThat(db.sql("SELECT count(*) FROM historico_execucao_frete WHERE frete_id = 910061")
                .query(Integer.class).single()).isEqualTo(inicios + cancelamentos);
        assertThat(db.sql("""
                SELECT count(*) FROM rf010_recibo_inicio r JOIN historico_execucao_frete h ON h.id = r.evento_id
                WHERE r.frete_id = 910061 AND h.tipo_evento = 'INICIO' AND h.usuario_autor_id = 910003
                """).query(Integer.class).single()).isEqualTo(inicios);
        assertThat(db.sql("""
                SELECT count(*) FROM historico_execucao_frete
                WHERE frete_id = 910061 AND tipo_evento = 'CANCELAMENTO' AND usuario_autor_id = 910001
                """).query(Integer.class).single()).isEqualTo(cancelamentos);
        assertThat(db.sql("SELECT count(*) FROM notificacao WHERE frete_id = 910061")
                .query(Integer.class).single()).isEqualTo(cancelamentos);
    }

    @Test
    void ocorrenciasComMesmoConteudoDistinguemReenvioDeNovaConfirmacao() throws Exception {
        var sessao = entrar("atual");
        String rota = ROTA + "/ocorrencias";
        String primeira = dados(UUID.randomUUID()).replace("justificativa", "descricao");
        var resposta = enviarComando(rota, sessao, primeira, 200).getResponse().getContentAsString();
        assertThat(enviarComando(rota, sessao, primeira, 200).getResponse().getContentAsString()).isEqualTo(resposta);
        enviarComando(rota, sessao, primeira.replace("Via bloqueada", "Outro registro"), 409);
        String nova = dados(UUID.randomUUID()).replace("justificativa", "descricao");
        var segunda = enviarComando(rota, sessao, nova, 200).getResponse().getContentAsString();
        assertThat(json.readTree(segunda).get("eventoId")).isNotEqualTo(json.readTree(resposta).get("eventoId"));
        assertThat(db.sql("""
                SELECT count(*) FROM rf010_recibo_ocorrencia r
                JOIN historico_execucao_frete h ON h.id = r.evento_id
                WHERE r.frete_id = 910063 AND h.tipo_evento = 'OCORRENCIA' AND h.descricao = 'Via bloqueada'
                """).query(Integer.class).single()).isEqualTo(2);
        mvc.perform(get(ROTA).session(sessao.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.eventos[0].descricao").value("Via bloqueada"));
    }

    @Test
    void motoristaConfirmaERecuperaARespostaDepoisDoCommit() throws Exception {
        Sessao sessao = entrar("atual");
        String comando = dados(UUID.randomUUID());
        var primeira = enviar(sessao, comando, 200);
        var repetida = enviar(sessao, comando, 200);
        assertThat(repetida.getResponse().getContentAsString())
                .isEqualTo(primeira.getResponse().getContentAsString());
        var resultado = json.readTree(repetida.getResponse().getContentAsString());
        assertThat(resultado.get("eventoId").asLong()).isEqualTo(910075L);
        assertThat(resultado.get("autorId").asLong()).isEqualTo(910003L);
        assertThat(resultado.get("estadoAnterior").asString()).isEqualTo("EM_EXECUCAO");
        assertThat(resultado.get("estadoResultante").asString()).isEqualTo("INTERROMPIDO");
        conferirBanco("INTERROMPIDO", 1);
        assertThat(db.sql("""
                SELECT data_hora_informada FROM rf010_recibo_interrupcao WHERE evento_id = 910075
                """).query(LocalDateTime.class).single()).isEqualTo(LocalDateTime.of(2000, 1, 2, 9, 15));
        assertThat(db.sql("""
                SELECT data_hora BETWEEN timezone('UTC', clock_timestamp()) - interval '1 minute'
                                     AND timezone('UTC', clock_timestamp())
                FROM historico_execucao_frete WHERE id = 910075
                """).query(Boolean.class).single()).isTrue();
        mvc.perform(get(ROTA).session(sessao.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INTERROMPIDO"))
                .andExpect(jsonPath("$.interrupcaoId").value(910075))
                .andExpect(jsonPath("$.eventos[0].justificativa").value("Via bloqueada"))
                .andExpect(jsonPath("$.eventos[0].autor").value("Motorista Atual"));
    }

    @Test
    void prestadorPodeConfirmarEChaveUsadaOuNovaTentativaGeramConflito() throws Exception {
        Sessao prestador = entrar("empresa");
        String comando = dados(UUID.randomUUID());
        var resposta = enviar(prestador, comando, 200);
        assertThat(json.readTree(resposta.getResponse().getContentAsString()).get("autorId").asLong())
                .isEqualTo(910002);
        enviar(prestador, comando.replace("Via bloqueada", "Outra justificativa"), 409);
        enviar(entrar("atual"), comando, 409);
        enviar(prestador, dados(UUID.randomUUID()), 409);
        conferirBanco("INTERROMPIDO", 1);
        assertThat(db.sql("""
                SELECT motorista_atual_id = 910011 AND veiculo_atual_id = 910022 AND revisao_designacao = 1
                FROM frete WHERE id = 910063
                """).query(Boolean.class).single()).isTrue();
    }

    @Test
    void duasConfirmacoesConcorrentesComChavesDistintasTemUmVencedor() throws Exception {
        concorrer(false);
    }

    @Test
    void confirmacaoConcorrenteComMesmaChaveRecuperaMesmoResultado() throws Exception {
        concorrer(true);
    }

    private void concorrer(boolean mesmaChave) throws Exception {
        Sessao atual = entrar("atual");
        Sessao outraSessao = entrar(mesmaChave ? "atual" : "empresa");
        String primeiro = dados(UUID.randomUUID());
        String segundo = mesmaChave ? primeiro : dados(UUID.randomUUID());
        var prontos = new CountDownLatch(2);
        var largada = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> enviarConcorrente(atual, primeiro, prontos, largada));
            var b = pool.submit(() -> enviarConcorrente(outraSessao, segundo, prontos, largada));
            assertThat(prontos.await(5, TimeUnit.SECONDS)).isTrue();
            largada.countDown();
            var respostaA = a.get(10, TimeUnit.SECONDS).getResponse();
            var respostaB = b.get(10, TimeUnit.SECONDS).getResponse();
            if (mesmaChave) {
                assertThat(respostaA.getStatus()).isEqualTo(200);
                assertThat(respostaB.getStatus()).isEqualTo(200);
                assertThat(respostaB.getContentAsString()).isEqualTo(respostaA.getContentAsString());
            } else {
                assertThat(new Integer[]{respostaA.getStatus(), respostaB.getStatus()})
                        .containsExactlyInAnyOrder(200, 409);
            }
        }
        conferirBanco("INTERROMPIDO", 1);
    }

    @Test
    void rejeitaColegaERevalidaAutorizacaoAposSubstituicaoAteNoReenvio() throws Exception {
        Sessao original = entrar("atual");
        String comando = dados(UUID.randomUUID());
        enviar(entrar("colega"), comando.replace("}", ", \"autorId\": 910003}"), 403);
        conferirBanco("EM_EXECUCAO", 0);
        enviar(original, comando, 200);
        db.sql("UPDATE frete SET motorista_atual_id = 910012, revisao_designacao = 2 WHERE id = 910063").update();
        enviar(original, comando, 403);
        enviar(original, dados(UUID.randomUUID()), 403);
        conferirBanco("INTERROMPIDO", 1);
    }

    @Test
    void formularioDeCicloAnteriorFalhaMasReenvioOriginalContinuaValidoAposRetomada() throws Exception {
        Sessao atual = entrar("atual");
        String original = dados(UUID.randomUUID());
        var resposta = enviar(atual, original, 200);
        retomar(atual, dadosRetomada(UUID.randomUUID(), 910075, true), 200);
        enviar(atual, dados(UUID.randomUUID()), 409);
        assertThat(enviar(atual, original, 200).getResponse().getContentAsString())
                .isEqualTo(resposta.getResponse().getContentAsString());
        conferirBanco("EM_EXECUCAO", 1);
    }

    @Test
    void falhaDoReciboReverteEstadoEEventoEPermiteReenvio() throws Exception {
        Sessao atual = entrar("atual");
        String comando = dados(UUID.randomUUID());
        db.sql("ALTER TABLE rf010_recibo_interrupcao ADD CONSTRAINT recusar_teste CHECK (false)").update();
        try {
            assertThatThrownBy(() -> enviar(atual, comando, 200))
                    .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class)
                    .hasCauseInstanceOf(DataIntegrityViolationException.class)
                    .hasStackTraceContaining("recusar_teste");
            conferirBanco("EM_EXECUCAO", 0);
        } finally {
            db.sql("ALTER TABLE rf010_recibo_interrupcao DROP CONSTRAINT recusar_teste").update();
        }
        enviar(atual, comando, 200);
        conferirBanco("INTERROMPIDO", 1);
    }

    @Test
    void exigeSessaoCsrfEJustificativa() throws Exception {
        String comando = dados(UUID.randomUUID());
        Sessao anonima = csrf(null);
        enviar(anonima, comando, 401);
        Sessao sessao = entrar("atual");
        mvc.perform(post(COMANDO).session(sessao.session()).contentType("application/json").content(comando))
                .andExpect(status().isForbidden());
        enviar(new Sessao(sessao.session(), sessao.cabecalho(), "csrf-invalido"), comando, 403);
        enviar(sessao, comando.replace("Via bloqueada", "\u00a0\u2003"), 400);
        enviar(sessao, comando.replace("09:15", "25:48"), 400);
        conferirBanco("EM_EXECUCAO", 0);
    }

    @Test
    void retomadaPeloPrestadorGravaVinculoEReenvioRetornaMesmoEvento() throws Exception {
        var motorista = entrar("atual");
        var prestador = entrar("empresa");
        enviar(motorista, dados(UUID.randomUUID()), 200);
        String comando = dadosRetomada(UUID.randomUUID(), 910075, true);
        var primeira = retomar(prestador, comando, 200);
        var reenvio = retomar(prestador, comando, 200);
        assertThat(reenvio.getResponse().getContentAsString()).isEqualTo(primeira.getResponse().getContentAsString());
        var resultado = json.readTree(reenvio.getResponse().getContentAsString());
        assertThat(resultado.get("eventoId").asLong()).isEqualTo(910076);
        assertThat(resultado.get("autorId").asLong()).isEqualTo(910002);
        assertThat(resultado.get("estadoResultante").asString()).isEqualTo("EM_EXECUCAO");
        assertThat(db.sql("""
                SELECT r.interrupcao_id = i.id AND i.tipo_evento = 'INTERRUPCAO'
                    AND i.justificativa = 'Via bloqueada' AND i.usuario_autor_id = 910003
                    AND e.usuario_autor_id = 910002 AND e.tipo_evento = 'RETOMADA'
                    AND e.justificativa = 'Via liberada'
                    AND e.data_hora >= i.data_hora
                    AND r.data_hora_informada = timestamp '2000-01-02 09:45'
                FROM rf010_recibo_retomada r
                JOIN historico_execucao_frete i ON i.id = r.interrupcao_id AND i.frete_id = r.frete_id
                JOIN historico_execucao_frete e ON e.id = r.evento_id AND e.frete_id = r.frete_id
                WHERE r.frete_id = 910063
                """).query(Boolean.class).single()).isTrue();
        retomar(prestador, comando.replace("Via liberada", "Outro motivo"), 409);
        retomar(motorista, comando, 409);
        retomar(prestador, dadosRetomada(UUID.randomUUID(), 910075, true), 409);
        mvc.perform(get(ROTA).session(motorista.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.cicloExecucaoId").value(910076))
                .andExpect(jsonPath("$.eventos[1].horarioRegistro").isString());
        conferirBanco("EM_EXECUCAO", 1);
        conferirRetomadas(1);
    }

    @Test
    void validaConfirmacaoReferenciaRecursosEAutorizacaoNoMomentoDaRetomada() throws Exception {
        Sessao atual = entrar("atual");
        Sessao colega = entrar("colega");
        UUID chaveInterrupcao = UUID.randomUUID();
        enviar(atual, dados(chaveInterrupcao), 200);
        String valido = dadosRetomada(UUID.randomUUID(), 910075, true);
        retomar(entrar("externo"), valido, 403);
        retomar(colega, valido, 403);
        retomar(atual, dadosRetomada(chaveInterrupcao, 910075, true), 409);
        retomar(atual, dadosRetomada(UUID.randomUUID(), 910072, true), 409);
        retomar(atual, valido.replace("true", "false"), 400);
        retomar(atual, valido.replace("Via liberada", " "), 400);
        retomar(atual, valido.replace("09:45", "29:45"), 400);
        retomar(atual, valido.replace("2000-01-02", "2000-02-31"), 400);
        mvc.perform(post(RETOMADA).session(atual.session()).contentType("application/json").content(valido))
                .andExpect(status().isForbidden());
        db.sql("UPDATE veiculo SET ativo = false WHERE id = 910022").update();
        retomar(atual, valido, 409);
        conferirBanco("INTERROMPIDO", 1);
        conferirRetomadas(0);
        db.sql("UPDATE veiculo SET ativo = true WHERE id = 910022").update();
        db.sql("UPDATE frete SET motorista_atual_id = 910012, revisao_designacao = 2 WHERE id = 910063").update();
        retomar(atual, valido, 403);
        retomar(colega, valido, 409);
        retomar(colega, valido.replace("\"revisaoDesignacao\":1", "\"revisaoDesignacao\":2"), 200);
        conferirBanco("EM_EXECUCAO", 1);
        conferirRetomadas(1);
    }

    @Test
    void falhaNoReciboDeRetomadaMantemAInterrupcaoEPermiteReenvio() throws Exception {
        Sessao motorista = entrar("atual");
        enviar(motorista, dados(UUID.randomUUID()), 200);
        String comando = dadosRetomada(UUID.randomUUID(), 910075, true);
        db.sql("ALTER TABLE rf010_recibo_retomada ADD CONSTRAINT recusar_retomada_teste CHECK (false)").update();
        try {
            assertThatThrownBy(() -> retomar(motorista, comando, 200))
                    .hasCauseInstanceOf(DataIntegrityViolationException.class)
                    .hasStackTraceContaining("recusar_retomada_teste");
            conferirBanco("INTERROMPIDO", 1);
            conferirRetomadas(0);
        } finally {
            db.sql("ALTER TABLE rf010_recibo_retomada DROP CONSTRAINT recusar_retomada_teste").update();
        }
        retomar(motorista, comando, 200);
        conferirBanco("EM_EXECUCAO", 1);
        conferirRetomadas(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"empresa", "atual"})
    void encerraSemCancelarNemConcluirEReenvioConservaEvento(String perfil) throws Exception {
        Sessao motorista = entrar("atual");
        enviar(motorista, dados(UUID.randomUUID()), 200);
        Sessao autor = perfil.equals("atual") ? motorista : entrar(perfil);
        String dados = dadosEncerramento(UUID.randomUUID(), 910075, true, true);
        var original = enviarComando(ENCERRAMENTO, autor, dados, 200).getResponse().getContentAsString();
        var repeticao = enviarComando(ENCERRAMENTO, autor, dados, 200).getResponse().getContentAsString();
        assertThat(repeticao).isEqualTo(original);
        long autorId = perfil.equals("atual") ? 910003 : 910002;
        assertThat(json.readTree(repeticao).get("autorId").asLong()).isEqualTo(autorId);
        assertThat(json.readTree(repeticao).get("eventoId").asLong()).isEqualTo(910076);
        assertThat(json.readTree(repeticao).get("estadoResultante").asString())
                .isEqualTo("ENCERRADO_SEM_CONCLUSAO");
        enviarComando(ENCERRAMENTO, autor, dados.replace("No veículo", "No depósito"), 409);
        conferirBanco("ENCERRADO_SEM_CONCLUSAO", 1);
        conferirEncerramentos(1);
        assertThat(db.sql("""
                SELECT r.interrupcao_id = 910075 AND r.evento_id = 910076
                    AND i.tipo_evento = 'INTERRUPCAO' AND i.justificativa = 'Via bloqueada'
                    AND e.usuario_autor_id = :autor AND e.tipo_evento = 'ENCERRAMENTO_SEM_CONCLUSAO'
                    AND e.justificativa = 'Impossível prosseguir' AND e.descricao IS NULL
                    AND r.situacao_encomenda = 'No veículo'
                    AND r.data_hora_informada = timestamp '2000-01-02 09:55'
                    AND e.data_hora >= i.data_hora
                FROM rf010_recibo_encerramento r
                JOIN historico_execucao_frete i ON i.id = r.interrupcao_id AND i.frete_id = r.frete_id
                JOIN historico_execucao_frete e ON e.id = r.evento_id AND e.frete_id = r.frete_id
                WHERE r.frete_id = 910063
                """).param("autor", autorId).query(Boolean.class).single()).isTrue();
        assertThat(db.sql("""
                SELECT data_hora_conclusao IS NULL AND responsavel_registro_conclusao_id IS NULL
                      AND nome_destinatario IS NULL AND cpf_destinatario IS NULL
                      AND valor_atual = 100 AND condicoes_atuais = 'Encomenda'
                FROM frete WHERE id = 910063
                """).query(Boolean.class).single()).isTrue();
        assertThat(db.sql("""
                SELECT count(*) FROM historico_execucao_frete WHERE frete_id = 910063
                    AND tipo_evento IN ('CONCLUSAO', 'CANCELAMENTO')
                """).query(Long.class).single()).isZero();
        var consulta = mvc.perform(get(ROTA).session(autor.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENCERRADO_SEM_CONCLUSAO"))
                .andExpect(jsonPath("$.eventos[0].tipo").value("ENCERRAMENTO_SEM_CONCLUSAO"))
                .andExpect(jsonPath("$.acoesElegiveis").isEmpty()).andReturn();
        assertThat(consulta.getResponse().getContentAsString()).doesNotContain("No veículo");
        var historico = mvc.perform(get("/api/historico-fretes").session(entrar("cliente").session())
                        .param("perfil", "CLIENTE"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(historico).contains("Encerrado sem conclusão", "Impossível prosseguir")
                .doesNotContain("No veículo", "Por Motorista Atual. No veículo");
    }

    @Test
    void exigeInterrupcaoConfirmacaoEAutorizacaoParaEncerrar() throws Exception {
        Sessao motorista = entrar("atual");
        String dados = dadosEncerramento(UUID.randomUUID(), 910075, true, false);
        enviarComando(ENCERRAMENTO, motorista, dados, 409);
        enviar(motorista, dados(UUID.randomUUID()), 200);
        enviarComando(ENCERRAMENTO, entrar("colega"), dados, 403);
        enviarComando(ENCERRAMENTO, csrf(null), dados, 401);
        mvc.perform(post(ENCERRAMENTO).session(motorista.session()).contentType("application/json").content(dados))
                .andExpect(status().isForbidden());
        enviarComando(ENCERRAMENTO, motorista, dados.replace("true", "false"), 400);
        enviarComando(ENCERRAMENTO, motorista, dados.replace("09:55", "28:00"), 400);
        enviarComando(ENCERRAMENTO, motorista, dados.replace("Impossível prosseguir", " "), 400);
        enviarComando(ENCERRAMENTO, motorista, dados.replace("910075", "910072"), 409);
        conferirBanco("INTERROMPIDO", 1);
        conferirEncerramentos(0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void retomadaEEncerramentoConcorrentesGravamSomenteOVencedor(boolean encerraPrimeiro) throws Exception {
        Sessao motorista = entrar("atual");
        Sessao prestador = entrar("empresa");
        enviar(motorista, dados(UUID.randomUUID()), 200);
        String retomar = dadosRetomada(UUID.randomUUID(), 910075, true);
        String encerrar = dadosEncerramento(UUID.randomUUID(), 910075, true, false);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var perdedora = new TransactionTemplate(transacoes).execute(ignorado -> {
                db.sql("SELECT id FROM frete WHERE id = 910063 FOR UPDATE").query(Long.class).single();
                var fila = executor.submit(() -> enviarComando(encerraPrimeiro ? RETOMADA : ENCERRAMENTO,
                        prestador, encerraPrimeiro ? retomar : encerrar, 409));
                aguardarDisputaNoPostgres();
                try {
                    enviarComando(encerraPrimeiro ? ENCERRAMENTO : RETOMADA,
                            motorista, encerraPrimeiro ? encerrar : retomar, 200);
                } catch (Exception erro) {
                    throw new AssertionError(erro);
                }
                return fila;
            });
            assertThat(perdedora.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(409);
        }
        conferirBanco(encerraPrimeiro ? "ENCERRADO_SEM_CONCLUSAO" : "EM_EXECUCAO", 1);
        conferirEncerramentos(encerraPrimeiro ? 1 : 0);
        conferirRetomadas(encerraPrimeiro ? 0 : 1);
    }

    @Test
    void sessaoCsrfEAutorDoContrato() throws Exception {
        var anonima = csrf(null);
        for (String rota : java.util.List.of(INICIO, ROTA + "/ocorrencias", COMANDO, RETOMADA, ENCERRAMENTO)) {
            enviarComando(rota, anonima, "{}", 401);
            mvc.perform(post(rota).contentType("application/json").content("{}")).andExpect(status().isForbidden());
            enviarComando(rota, new Sessao(anonima.session(), anonima.cabecalho(), "invalido"), "{}", 403);
        }
        var motorista = entrar("atual");
        var dados = dados(UUID.randomUUID()).replace("}", ", \"autorId\":910002, \"papel\":\"PRESTADOR\"}");
        var resposta = enviar(motorista, dados, 200);
        var evento = json.readTree(resposta.getResponse().getContentAsString());
        assertThat(evento.get("autorId").asLong()).isEqualTo(910003);
        assertThat(db.sql("SELECT usuario_autor_id FROM historico_execucao_frete WHERE id = :id")
                .param("id", evento.get("eventoId").asLong()).query(Long.class).single()).isEqualTo(910003);
    }

    private record ComandoTeste(String rota, String corpo) {}

    private java.util.List<ComandoTeste> comandosDoCiclo() {
        return java.util.List.of(
                new ComandoTeste(ROTA + "/inicio", dadosInicio()),
                new ComandoTeste(ROTA + "/ocorrencias", dados(UUID.randomUUID()).replace("justificativa", "descricao")),
                new ComandoTeste(COMANDO, dados(UUID.randomUUID())),
                new ComandoTeste(RETOMADA, dadosRetomada(UUID.randomUUID(), 910075, true)),
                new ComandoTeste(ENCERRAMENTO, dadosEncerramento(UUID.randomUUID(), 910075, true, false)));
    }

    private void conferirSemEscrita(String estado, long eventos) {
        assertThat(db.sql("SELECT status FROM frete WHERE id = 910063").query(String.class).single()).isEqualTo(estado);
        assertThat(db.sql("SELECT count(*) FROM historico_execucao_frete WHERE frete_id = 910063")
                .query(Long.class).single()).isEqualTo(eventos);
        assertThat(db.sql("SELECT count(*) FROM notificacao WHERE frete_id = 910063").query(Long.class).single()).isZero();
    }

    @Test
    void cadaRecusaPreservaEstadoEventosEAvisos() throws Exception {
        var motorista = entrar("atual");
        for (String estado : java.util.List.of("CANCELADO", "CONCLUIDO", "ENCERRADO_SEM_CONCLUSAO", "AGUARDANDO_DESIGNACAO")) {
            db.sql("""
                    UPDATE frete SET status = :estado, data_hora_conclusao = CURRENT_TIMESTAMP,
                    responsavel_registro_conclusao_id = 910002 WHERE id = 910063
                    """).param("estado", estado).update();
            for (var cmd : comandosDoCiclo()) {
                enviarComando(cmd.rota(), motorista, cmd.corpo(), 409);
                conferirSemEscrita(estado, 2);
            }
        }
        db.sql("UPDATE frete SET status = 'EM_EXECUCAO', data_hora_conclusao = NULL, responsavel_registro_conclusao_id = NULL WHERE id = 910063").update();
        for (var cmd : comandosDoCiclo()) {
            if (!cmd.rota().equals(RETOMADA) && !cmd.rota().equals(ENCERRAMENTO)) continue;
            enviarComando(cmd.rota(), motorista, cmd.corpo(), 409);
            conferirSemEscrita("EM_EXECUCAO", 2);
        }
        enviar(motorista, dados(UUID.randomUUID()), 200);
        for (var cmd : comandosDoCiclo()) {
            if (!cmd.rota().equals(RETOMADA) && !cmd.rota().equals(ENCERRAMENTO)) continue;
            for (String invalido : java.util.List.of(cmd.corpo().replace("true", "null"),
                    cmd.corpo().replace("Via liberada", " ").replace("Impossível prosseguir", " "))) {
                enviarComando(cmd.rota(), motorista, invalido, 400);
                conferirSemEscrita("INTERROMPIDO", 3);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void falhaNoReciboReverteOcorrenciaOuEncerramento(boolean encerrar) throws Exception {
        var motorista = entrar("atual");
        if (encerrar) enviar(motorista, dados(UUID.randomUUID()), 200);
        String tabela = encerrar ? "rf010_recibo_encerramento" : "rf010_recibo_ocorrencia";
        String rota = encerrar ? ENCERRAMENTO : ROTA + "/ocorrencias";
        String comando = encerrar ? dadosEncerramento(UUID.randomUUID(), 910075, true, false)
                : dados(UUID.randomUUID()).replace("justificativa", "descricao");
        db.sql("ALTER TABLE " + tabela + " ADD CONSTRAINT recusar_gravacao CHECK (false)").update();
        try {
            assertThatThrownBy(() -> enviarComando(rota, motorista, comando, 200))
                    .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class).hasStackTraceContaining("recusar_gravacao");
            conferirSemEscrita(encerrar ? "INTERROMPIDO" : "EM_EXECUCAO", encerrar ? 3 : 2);
        } finally {
            db.sql("ALTER TABLE " + tabela + " DROP CONSTRAINT recusar_gravacao").update();
        }
        var sucesso = enviarComando(rota, motorista, comando, 200).getResponse().getContentAsString();
        assertThat(enviarComando(rota, motorista, comando, 200).getResponse().getContentAsString()).isEqualTo(sucesso);
    }

    @Test
    void formulariosDeDuasInterrupcoesComMesmaDesignacao() throws Exception {
        var motorista = entrar("atual");
        var antigos = comandosDoCiclo();
        enviar(motorista, dados(UUID.randomUUID()), 200);
        retomar(motorista, dadosRetomada(UUID.randomUUID(), 910075, true), 200);
        for (var cmd : antigos) {
            if (cmd.rota().equals(COMANDO) || cmd.rota().endsWith("/ocorrencias")) {
                enviarComando(cmd.rota(), motorista, cmd.corpo(), 409);
                conferirSemEscrita("EM_EXECUCAO", 4);
            }
        }
        enviar(motorista, dados(UUID.randomUUID()).replace("910073", "910076"), 200);
        for (var cmd : antigos) {
            if (cmd.rota().equals(RETOMADA) || cmd.rota().equals(ENCERRAMENTO)) {
                enviarComando(cmd.rota(), motorista, cmd.corpo(), 409);
                conferirSemEscrita("INTERROMPIDO", 5);
            }
        }
    }

    private void aguardarDisputaNoPostgres() {
        long limite = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < limite) {
            if (db.sql("""
                    SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE pg_backend_pid() = ANY(pg_blocking_pids(pid)))
                    """).query(Boolean.class).single()) return;
            try {
                Thread.sleep(20);
            } catch (InterruptedException erro) {
                Thread.currentThread().interrupt();
                throw new AssertionError(erro);
            }
        }
        throw new AssertionError("A segunda tentativa não aguardou o bloqueio do frete.");
    }

    private void conferirEncerramentos(long quantidade) {
        assertThat(db.sql("""
                SELECT count(*) FROM historico_execucao_frete
                WHERE frete_id = 910063 AND tipo_evento = 'ENCERRAMENTO_SEM_CONCLUSAO'
                """).query(Long.class).single()).isEqualTo(quantidade);
        assertThat(db.sql("SELECT count(*) FROM rf010_recibo_encerramento WHERE frete_id = 910063")
                .query(Long.class).single()).isEqualTo(quantidade);
    }

    private static String dadosEncerramento(UUID chave, long interrupcaoId, boolean confirmou, boolean comSituacao) {
        return """
                {"chaveOperacao":"%s","estadoEsperado":"INTERROMPIDO","revisaoDesignacao":1,
                 "interrupcaoId":%d,"justificativa":"Impossível prosseguir","data":"2000-01-02",
                 "horario":"09:55","confirmou":%s%s}
                """.formatted(chave, interrupcaoId, confirmou, comSituacao ? ", \"situacaoEncomenda\": \"No veículo\"" : "");
    }

    private MvcResult enviarComando(String rota, Sessao sessao, String dados, int status) throws Exception {
        return mvc.perform(post(rota).session(sessao.session()).header(sessao.cabecalho(), sessao.token())
                .contentType("application/json").content(dados))
                .andExpect(status().is(status)).andReturn();
    }

    private void conferirRetomadas(long quantidade) {
        assertThat(db.sql("""
                SELECT count(*) FROM historico_execucao_frete WHERE frete_id = 910063 AND tipo_evento = 'RETOMADA'
                """).query(Long.class).single()).isEqualTo(quantidade);
        assertThat(db.sql("SELECT count(*) FROM rf010_recibo_retomada WHERE frete_id = 910063")
                .query(Long.class).single()).isEqualTo(quantidade);
    }

    private static String dadosRetomada(UUID chave, long interrupcaoId, boolean confirmou) {
        return """
                {"chaveOperacao":"%s","estadoEsperado":"INTERROMPIDO","revisaoDesignacao":1,
                 "interrupcaoId":%d,"justificativa":"Via liberada","data":"2000-01-02","horario":"09:45","confirmou":%s}
                """.formatted(chave, interrupcaoId, confirmou);
    }

    private MvcResult retomar(Sessao sessao, String dados, int status) throws Exception {
        return enviarComando(RETOMADA, sessao, dados, status);
    }

    private void conferirBanco(String estado, long quantidade) {
        assertThat(db.sql("SELECT status FROM frete WHERE id = 910063").query(String.class).single())
                .isEqualTo(estado);
        assertThat(db.sql("""
                SELECT count(*) FROM historico_execucao_frete WHERE frete_id = 910063 AND tipo_evento = 'INTERRUPCAO'
                """).query(Long.class).single()).isEqualTo(quantidade);
        assertThat(db.sql("SELECT count(*) FROM rf010_recibo_interrupcao WHERE frete_id = 910063")
                .query(Long.class).single()).isEqualTo(quantidade);
        assertThat(db.sql("SELECT count(*) FROM notificacao").query(Long.class).single()).isZero();
    }

    private static String dados(UUID chave) {
        return """
                {"chaveOperacao":"%s","estadoEsperado":"EM_EXECUCAO","revisaoDesignacao":1,
                 "cicloExecucaoId":910073,"justificativa":"Via bloqueada","data":"2000-01-02","horario":"09:15"}
                """.formatted(chave);
    }

    private MvcResult enviar(Sessao sessao, String dados, int status) throws Exception {
        return enviar(sessao, dados).andExpect(status().is(status)).andReturn();
    }

    private MvcResult enviarConcorrente(Sessao sessao, String dados, CountDownLatch prontos,
                                         CountDownLatch largada) throws Exception {
        prontos.countDown();
        if (!largada.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Teste não iniciou");
        return enviar(sessao, dados).andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions enviar(Sessao sessao, String dados) throws Exception {
        return mvc.perform(post(COMANDO).session(sessao.session()).header(sessao.cabecalho(), sessao.token())
                .contentType("application/json").content(dados));
    }

    private record Sessao(MockHttpSession session, String cabecalho, String token) {}

    private Sessao entrar(String conta) throws Exception {
        String email = conta + "910@example.test";
        db.sql("UPDATE usuario SET senha = :senha WHERE email = :email")
                .param("senha", senhas.encode("Teste_RF010#")).param("email", email).update();
        var csrf = csrf(null);
        var login = mvc.perform(post("/api/auth/login").session(csrf.session())
                        .header(csrf.cabecalho(), csrf.token()).param("email", email).param("senha", "Teste_RF010#"))
                .andExpect(status().isNoContent()).andReturn();
        return csrf((MockHttpSession) login.getRequest().getSession(false));
    }

    private Sessao csrf(MockHttpSession session) throws Exception {
        var req = get("/api/auth/csrf");
        if (session != null) req.session(session);
        var resposta = mvc.perform(req).andExpect(status().isOk()).andReturn();
        var token = json.readTree(resposta.getResponse().getContentAsString());
        return new Sessao((MockHttpSession) resposta.getRequest().getSession(false),
                token.get("headerName").asString(), token.get("token").asString());
    }
}
