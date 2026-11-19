package com.muv.backend.controller;

import com.muv.backend.service.FreightCancellationService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("rf011-test")
@Sql("/db/rf011-fixture.sql")
class FreightCancellationIntegrationTest {
    private static final String SCHEMA = "rf011_" + UUID.randomUUID().toString().replace("-", "");
    private static final String PATH = "/api/fretes/11010/cancelamento";
    @DynamicPropertySource
    static void isolatedSchema(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcClient db;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords;
    @Autowired FreightCancellationService service;
    @Autowired PlatformTransactionManager transactions;

    @ParameterizedTest
    @ValueSource(strings = {"AGUARDANDO_DESIGNACAO", "PRONTO_INICIO"})
    void clientCancelsWithSessionAndPersistsEventAndNotification(String state) throws Exception {
        state(state);
        var session = login("cliente");
        mvc.perform(get(PATH).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.autorPapel").value("Cliente"));
        send(session, "{\"justificativa\":\"  Mudança de planos  \",\"autorId\":11003}", 204);
        assertCancellation(11001, 11002);
        mvc.perform(get(PATH).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.frete.status").value("CANCELADO"))
                .andExpect(jsonPath("$.registro.autor").value("Cliente RF011"))
                .andExpect(jsonPath("$.registro.justificativa").value("Mudança de planos"))
                .andExpect(jsonPath("$.registro.dataHora").isNotEmpty());
        send(session, "{\"justificativa\":\"Segundo envio\"}", 409);
        assertCancellation(11001, 11002);
    }

    @Test
    void providerNotifiesOnlyClient() throws Exception {
        send(login("prestador"), "{\"justificativa\":\"Mudança de planos\"}", 204);
        assertCancellation(11002, 11001);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"justificativa\":null}", "{\"justificativa\":\"\"}",
            "{\"justificativa\":\"  \\t\\n\"}", "{\"justificativa\":\"\u00a0\u2003\"}"})
    void rejectsBlankReasonWithoutWrites(String body) throws Exception {
        send(login("cliente"), body, 400);
        assertUnchanged("PRONTO_INICIO");
    }

    @Test
    void requiresSessionCsrfAndRelationship() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        send(new MockHttpSession(), "{\"justificativa\":\"Motivo\"}", 401);
        var third = login("terceiro");
        mvc.perform(get(PATH).session(third)).andExpect(status().isForbidden());
        send(third, "{\"justificativa\":\"Motivo\",\"autorId\":11001}", 403);
        mvc.perform(post(PATH).session(login("cliente")).contentType("application/json")
                .content("{\"justificativa\":\"Motivo\"}")).andExpect(status().isForbidden());
        assertUnchanged("PRONTO_INICIO");
    }

    @Test
    void anonymousPostWithMissingOrInvalidCsrfIsRejectedBeforeAuthentication() throws Exception {
        String body = "{\"justificativa\":\"Motivo\"}";
        mvc.perform(post(PATH).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        var csrf = csrf(new MockHttpSession());
        mvc.perform(post(PATH).session(csrf.session()).header(csrf.header(), "token-invalido")
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        assertUnchanged("PRONTO_INICIO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"EM_EXECUCAO", "INTERROMPIDO", "CONCLUIDO", "ENCERRADO_SEM_CONCLUSAO", "CANCELADO"})
    void rejectsStateChangedAfterOpeningPage(String state) throws Exception {
        var session = login("cliente");
        mvc.perform(get(PATH).session(session)).andExpect(status().isOk());
        state(state);
        send(session, "{\"justificativa\":\"Motivo\"}", 409);
        assertUnchanged(state);
    }

    @Test
    void missingFreightReturns404() throws Exception {
        mvc.perform(get("/api/fretes/99999/cancelamento").session(login("cliente")))
                .andExpect(status().isNotFound());
    }

    @Test
    void notificationFailureRollsBackFreightAndHistory() {
        // Real PostgreSQL constraint failure after UPDATE and history INSERT have executed.
        db.sql("ALTER TABLE notificacao ADD CONSTRAINT rf011_fail CHECK (titulo <> 'Frete cancelado')").update();
        try {
            assertThatThrownBy(() -> service.cancel(11010, 11001, "Mudança de planos"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertUnchanged("PRONTO_INICIO");
        } finally {
            db.sql("ALTER TABLE notificacao DROP CONSTRAINT rf011_fail").update();
        }
    }

    @Test
    void simultaneousCancellationsProduceOneEventAndNotification() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Callable<Integer> cancel = () -> {
                start.await(5, TimeUnit.SECONDS);
                try { service.cancel(11010, 11001, "Mudança de planos"); return 204; }
                catch (ResponseStatusException error) { return error.getStatusCode().value(); }
            };
            var first = executor.submit(cancel);
            var second = executor.submit(cancel);
            start.countDown();
            assertThat(java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(204, 409);
            assertCancellation(11001, 11002);
        }
    }

    @Test
    void recordsSameUtcTimeAfterWaitingForFreightLock() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            var cancellation = new AtomicReference<Future<?>>();
            var beforeUnlockUtc = new TransactionTemplate(transactions).execute(tx -> {
                db.sql("SELECT id FROM frete WHERE id = 11010 FOR UPDATE").query(Long.class).single();
                cancellation.set(executor.submit(() ->
                        new TransactionTemplate(transactions).executeWithoutResult(cancelTx -> {
                            db.sql("SET LOCAL TIME ZONE 'America/Fortaleza'").update();
                            service.cancel(11010, 11001, "Mudança de planos");
                        })));
                waitForBlockedTransaction();
                return db.sql("SELECT timezone('UTC', clock_timestamp())").query(LocalDateTime.class).single();
            });
            cancellation.get().get(10, TimeUnit.SECONDS);
            LocalDateTime afterCancellationUtc = db.sql("SELECT timezone('UTC', clock_timestamp())")
                    .query(LocalDateTime.class).single();
            LocalDateTime eventTime = db.sql("SELECT data_hora FROM historico_execucao_frete WHERE frete_id = 11010")
                    .query(LocalDateTime.class).single();
            assertThat(eventTime).isBetween(beforeUnlockUtc, afterCancellationUtc);
            assertCancellation(11001, 11002);
        }
    }

    @Test
    void committedStartWinsAgainstCancellationWaitingOnSameRow() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = new AtomicReference<Future<Integer>>();
            new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                db.sql("SELECT id FROM frete WHERE id = 11010 FOR UPDATE").query(Long.class).single();
                future.set(executor.submit(() -> {
                    try { service.cancel(11010, 11001, "Mudança de planos"); return 204; }
                    catch (ResponseStatusException error) { return error.getStatusCode().value(); }
                }));
                waitForBlockedTransaction();
                assertThat(startExecution()).isEqualTo(1);
            });
            assertThat(future.get().get(10, TimeUnit.SECONDS)).isEqualTo(409);
            assertUnchanged("EM_EXECUCAO");
        }
    }

    @Test
    void committedCancellationPreventsConcurrentConditionalStart() throws Exception {
        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = new AtomicReference<Future<Integer>>();
            new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                service.cancel(11010, 11001, "Mudança de planos");
                future.set(executor.submit(() -> new TransactionTemplate(transactions).execute(ignored -> startExecution())));
                waitForBlockedTransaction();
            });
            assertThat(future.get().get(10, TimeUnit.SECONDS)).isZero();
            assertCancellation(11001, 11002);
        }
    }

    private int startExecution() {
        // RF010 is absent: exercise its required compare-and-set contract directly.
        return db.sql("UPDATE frete SET status = 'EM_EXECUCAO' WHERE id = 11010 AND status = 'PRONTO_INICIO'")
                .update();
    }

    private void waitForBlockedTransaction() {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (db.sql("SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE pg_backend_pid() = ANY(pg_blocking_pids(pid)))")
                    .query(Boolean.class).single()) return;
            try { Thread.sleep(20); } catch (InterruptedException error) {
                Thread.currentThread().interrupt(); throw new AssertionError(error);
            }
        }
        throw new AssertionError("A segunda transação não aguardou o bloqueio do frete.");
    }

    private void state(String state) {
        db.sql("UPDATE frete SET status = :state, data_hora_conclusao = CURRENT_TIMESTAMP, responsavel_registro_conclusao_id = 11002 WHERE id = 11010")
                .param("state", state).update();
    }

    private void assertCancellation(long author, long recipient) {
        assertThat(db.sql("SELECT status FROM frete WHERE id = 11010").query(String.class).single()).isEqualTo("CANCELADO");
        var event = db.sql("SELECT * FROM historico_execucao_frete WHERE frete_id = 11010").query().singleRow();
        assertThat(event.get("usuario_autor_id")).isEqualTo(author);
        assertThat(event.get("tipo_evento")).isEqualTo("CANCELAMENTO");
        assertThat(event.get("justificativa")).isEqualTo("Mudança de planos");
        assertThat(event.get("data_hora")).isNotNull();
        assertThat(event.get("id")).isNotNull();
        var notification = db.sql("SELECT * FROM notificacao WHERE frete_id = 11010").query().singleRow();
        assertThat(notification.get("usuario_destino_id")).isEqualTo(recipient);
        assertThat(notification.get("lida")).isEqualTo(false);
        assertThat(notification.get("criado_em")).isEqualTo(event.get("data_hora"));
        assertThat(db.sql("SELECT valor_atual FROM frete WHERE id = 11010").query(java.math.BigDecimal.class).single())
                .isEqualByComparingTo("350.00");
        assertThat(db.sql("SELECT condicoes_atuais FROM frete WHERE id = 11010").query(String.class).single())
                .isEqualTo("Condições originais");
    }

    private void assertUnchanged(String state) {
        assertThat(db.sql("SELECT status FROM frete WHERE id = 11010").query(String.class).single()).isEqualTo(state);
        assertThat(db.sql("SELECT count(*) FROM historico_execucao_frete").query(Long.class).single()).isZero();
        assertThat(db.sql("SELECT count(*) FROM notificacao").query(Long.class).single()).isZero();
    }

    private MockHttpSession login(String profile) throws Exception {
        String email = profile + ".rf011@example.test";
        db.sql("UPDATE usuario SET senha = :password WHERE email = :email")
                .param("password", passwords.encode("TesteRF011!42")).param("email", email).update();
        var csrf = csrf(new MockHttpSession());
        var result = mvc.perform(post("/api/auth/login").session(csrf.session()).header(csrf.header(), csrf.token())
                .param("email", email).param("senha", "TesteRF011!42"))
                .andExpect(status().isNoContent()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void send(MockHttpSession session, String body, int expected) throws Exception {
        var csrf = csrf(session);
        mvc.perform(post(PATH).session(session).header(csrf.header(), csrf.token())
                .contentType("application/json").content(body)).andExpect(status().is(expected));
    }

    private Csrf csrf(MockHttpSession session) throws Exception {
        var result = mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        return new Csrf(session, body.get("headerName").stringValue(), body.get("token").stringValue());
    }
    private record Csrf(MockHttpSession session, String header, String token) {}
}
