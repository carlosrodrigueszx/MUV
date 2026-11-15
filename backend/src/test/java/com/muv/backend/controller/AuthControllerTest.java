package com.muv.backend.controller;

import com.muv.backend.service.AuthService.RecoveryMail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("rf001-local")
@Transactional
@RecordApplicationEvents
class AuthControllerTest {
    private static final String EMAIL = "passageiro@example.test";
    private static final String PASSWORD = "SenhaSintetica!24";
    private static final String CPF = "123.456.789-09";
    private static final String REGISTRATION = """
            {"nome":"Passageiro de Teste","cpf":"%s","telefone":"85900000000",
             "email":"%s","genero":"Prefiro não informar","nascimento":"1994-06-18",
             "senha":"%s","confirmarSenha":"%s","isAdmin":true,"perfil":"PRESTADOR"}
            """.formatted(CPF, EMAIL, PASSWORD, PASSWORD);

    @Autowired MockMvc mvc;
    @Autowired JdbcClient db;
    @Autowired ObjectMapper json;
    @Autowired ApplicationEvents events;

    @Test
    void savesPassengerWithoutProviderOrAdminPrivileges() throws Exception {
        register();
        var account = db.sql("""
                SELECT u.senha, u.is_admin, p.cpf, p.cpf_indice, p.dados_cifrados,
                       EXISTS (SELECT 1 FROM prestador WHERE id = u.id) AS prestador
                FROM usuario u JOIN passageiro p ON p.id = u.id WHERE u.email = :email
                """).param("email", EMAIL).query().singleRow();
        assertThat(account.get("senha")).asString().startsWith("$2").doesNotContain(PASSWORD);
        assertThat(account.get("is_admin")).isEqualTo(false);
        assertThat(account.get("prestador")).isEqualTo(false);
        assertThat(account.get("cpf")).isNull();
        assertThat(account.get("cpf_indice")).asString().hasSize(64);
        assertThat(account.get("dados_cifrados")).asString().doesNotContain(CPF, "12345678909", "1994-06-18");
    }

    @Test
    void rejectsDuplicateEmailIgnoringCaseAndDuplicateCpfIgnoringFormat() throws Exception {
        register();
        send("cadastro", REGISTRATION.replace(EMAIL, EMAIL.toUpperCase()).replace(CPF, "98765432100"), 409);
        send("cadastro", REGISTRATION.replace(EMAIL, "outro@example.test").replace(CPF, "12345678909"), 409);
    }

    @Test
    void rejectsInvalidRegistrationAndMissingCsrf() throws Exception {
        send("cadastro", REGISTRATION.replace(CPF, "111.111.111-11"), 400);
        send("cadastro", REGISTRATION.replace("\"confirmarSenha\":\"" + PASSWORD + "\"", "\"confirmarSenha\":\"Diferente!42\""), 400);
        send("cadastro", REGISTRATION.replace(EMAIL, "invalido"), 400);
        mvc.perform(post("/api/auth/cadastro").contentType("application/json").content(REGISTRATION))
                .andExpect(status().isForbidden());
        assertThat(db.sql("SELECT COUNT(*) FROM usuario WHERE email = :email").param("email", EMAIL)
                .query(Long.class).single()).isZero();
    }

    @Test
    void createsAuthenticatedSessionOnlyWithValidCredentialsAndLogsOut() throws Exception {
        register();
        mvc.perform(get("/api/auth/sessao")).andExpect(status().isUnauthorized());
        login("senhaErrada!", 401);
        MockHttpSession session = login(PASSWORD, 204);
        mvc.perform(get("/api/auth/sessao").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.senha").doesNotExist());
        Csrf csrf = csrf(session);
        mvc.perform(post("/api/auth/logout").session(csrf.session()).header(csrf.header(), csrf.token()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/sessao")).andExpect(status().isUnauthorized());
    }

    @Test
    void resetsPasswordOnceAndDoesNotRevealUnknownAccounts() throws Exception {
        register();
        String unknown = send("recuperacao", "{\"email\":\"inexistente@example.test\"}", 202);
        assertThat(events.stream(RecoveryMail.class).count()).isZero();
        String known = send("recuperacao", "{\"email\":\"" + EMAIL + "\"}", 202);
        assertThat(known).isEqualTo(unknown);
        String token = events.stream(RecoveryMail.class).findFirst().orElseThrow().token();
        assertThat(db.sql("SELECT token FROM token_recuperacao WHERE id_usuario = (SELECT id FROM usuario WHERE email = :email)")
                .param("email", EMAIL).query(String.class).single()).hasSize(64).isNotEqualTo(token);
        String payload = resetPayload(token);
        send("redefinicao", payload, 204);
        send("redefinicao", payload, 400);
        login(PASSWORD, 401);
        login("NovaSintetica!24", 204);
    }

    @Test
    void rejectsExpiredReset() throws Exception {
        register();
        send("recuperacao", "{\"email\":\"" + EMAIL + "\"}", 202);
        String token = events.stream(RecoveryMail.class).findFirst().orElseThrow().token();
        db.sql("UPDATE token_recuperacao SET expira_em = TIMESTAMP '2000-01-01' WHERE id_usuario = (SELECT id FROM usuario WHERE email = :email)")
                .param("email", EMAIL).update();
        send("redefinicao", resetPayload(token), 400);
        login(PASSWORD, 204);
    }

    private void register() throws Exception { send("cadastro", REGISTRATION, 201); }

    private String send(String path, String body, int expected) throws Exception {
        Csrf csrf = csrf(new MockHttpSession());
        return mvc.perform(post("/api/auth/" + path).session(csrf.session()).header(csrf.header(), csrf.token())
                        .contentType("application/json").content(body))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
    }

    private Csrf csrf(MockHttpSession session) throws Exception {
        var result = mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        return new Csrf(session, body.get("headerName").stringValue(), body.get("token").stringValue());
    }

    private MockHttpSession login(String password, int expected) throws Exception {
        Csrf csrf = csrf(new MockHttpSession());
        var result = mvc.perform(post("/api/auth/login").session(csrf.session()).header(csrf.header(), csrf.token())
                        .contentType("application/x-www-form-urlencoded").param("email", EMAIL).param("senha", password))
                .andExpect(status().is(expected)).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String resetPayload(String token) {
        return """
                {"token":"%s","senha":"NovaSintetica!24","confirmarSenha":"NovaSintetica!24"}
                """.formatted(token);
    }

    private record Csrf(MockHttpSession session, String header, String token) {}
}
