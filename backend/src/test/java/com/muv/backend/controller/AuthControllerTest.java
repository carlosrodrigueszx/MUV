package com.muv.backend.controller;

import com.muv.backend.service.AuthService.RecoveryMail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import com.muv.backend.repository.AuthRepository;
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
             "senha":"%s","confirmarSenha":"%s","aceitouTermos":true,"isAdmin":true,"perfil":"PRESTADOR"}
            """.formatted(CPF, EMAIL, PASSWORD, PASSWORD);

    @Autowired MockMvc mvc;
    @Autowired JdbcClient db;
    @Autowired AuthRepository accounts;
    @Autowired ObjectMapper json;
    @Autowired ApplicationEvents events;

    @Test
    void commonAccountCreationDoesNotRecordAnAcceptance() {
        long id = accounts.insertAccount("Conta de teste", "85900000000", "comum@example.test", "hash");
        var account = db.sql("SELECT termos_aceitos_em FROM usuario WHERE id = :id")
                .param("id", id).query().singleRow();
        assertThat(account.get("termos_aceitos_em")).isNull();
    }

    @Test
    void savesPassengerWithoutProviderOrAdminPrivileges() throws Exception {
        register();
        var account = db.sql("""
                SELECT u.senha, u.is_admin, u.termos_aceitos_em, p.cpf, p.cpf_indice, p.dados_cifrados,
                       EXISTS (SELECT 1 FROM prestador WHERE id = u.id) AS prestador
                FROM usuario u JOIN passageiro p ON p.id = u.id WHERE u.email = :email
                """).param("email", EMAIL).query().singleRow();
        assertThat(account.get("senha")).asString().startsWith("$2").doesNotContain(PASSWORD);
        assertThat(account.get("termos_aceitos_em")).isNotNull();
        assertThat(account.get("is_admin")).isEqualTo(false);
        assertThat(account.get("prestador")).isEqualTo(false);
        assertThat(account.get("cpf")).isNull();
        assertThat(account.get("cpf_indice")).asString().hasSize(64);
        assertThat(account.get("dados_cifrados")).asString().doesNotContain(CPF, "12345678909", "1994-06-18");
    }

    @Test
    void registersCompanyWithoutAuthorizingModalities() throws Exception {
        String company = """
                {"razaoSocial":"Empresa de Teste","nomeFantasia":"Empresa",
                 "cnpj":"12.345.678/0001-95","telefone":"85900000000","email":"empresa@example.test",
                 "senha":"%s","confirmarSenha":"%s","aceitouTermos":true,"declarouConformidade":true}
                """.formatted(PASSWORD, PASSWORD);
        send("cadastro/empresa", company.replace("aceitouTermos", "campoAusente"), 400);
        send("cadastro/empresa", company, 201);
        send("cadastro/empresa", company.replace("empresa@example.test", "outra@example.test")
                .replace("12.345.678/0001-95", "12345678000195"), 409);

        assertThat(db.sql("""
                SELECT COUNT(*) FROM usuario u
                JOIN prestador p ON p.id = u.id JOIN prestador_empresa e ON e.id = u.id
                WHERE u.email = 'empresa@example.test' AND u.termos_aceitos_em IS NOT NULL
                  AND p.conformidade_declarada_em IS NOT NULL AND NOT u.is_admin
                  AND NOT EXISTS (SELECT 1 FROM autorizacao_modalidade a WHERE a.id_prestador = p.id)
                """).query(Long.class).single()).isEqualTo(1L);
    }

    @Test
    void registersIndependentWithOneAccountAndEncryptedLicense() throws Exception {
        String independent = """
                {"conta":%s,"cnh":"12345678901","categoriaCnh":"D","declarouConformidade":true}
                """.formatted(REGISTRATION);
        send("cadastro/autonomo", independent.replace("declarouConformidade", "campoAusente"), 400);
        send("cadastro/autonomo", independent, 201);
        send("cadastro", REGISTRATION, 409);
        login(PASSWORD, 204);

        assertThat(db.sql("""
                SELECT a.habilitacao_cifrada FROM usuario u JOIN prestador_autonomo a ON a.id = u.id
                JOIN passageiro p ON p.id = u.id WHERE u.email = :email
                """).param("email", EMAIL).query(String.class).single()).doesNotContain("12345678901");
    }

    @Test
    void activatesIndependentProfileOnTheAuthenticatedPassengerAccount() throws Exception {
        register();
        long id = accounts.findAccount(EMAIL).orElseThrow().id();
        String activation = """
                {"cnh":"12345678901","categoriaCnh":"D","declarouConformidade":true}
                """;
        send("perfil/autonomo", activation, 401);
        MockHttpSession session = login(PASSWORD, 204);
        mvc.perform(get("/api/auth/sessao").session(session))
                .andExpect(jsonPath("$.podeAtivarAutonomo").value(true));
        mvc.perform(post("/api/auth/perfil/autonomo").session(session)
                        .contentType("application/json").content(activation))
                .andExpect(status().isForbidden());
        send(session, "perfil/autonomo", activation.replace("true", "false"), 400);
        send(session, "perfil/autonomo", activation, 204);
        send(session, "perfil/autonomo", activation, 409);

        mvc.perform(get("/api/auth/sessao").session(session))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.podeAtivarAutonomo").value(false));
        var profile = db.sql("""
                SELECT a.habilitacao_cifrada, p.conformidade_declarada_em,
                       EXISTS (SELECT 1 FROM autorizacao_modalidade WHERE id_prestador = p.id) AS autorizado
                FROM passageiro c JOIN prestador p ON p.id = c.id
                JOIN prestador_autonomo a ON a.id = p.id WHERE c.id = :id
                """).param("id", id).query().singleRow();
        assertThat(profile.get("habilitacao_cifrada")).asString().doesNotContain("12345678901");
        assertThat(profile.get("conformidade_declarada_em")).isNotNull();
        assertThat(profile.get("autorizado")).isEqualTo(false);
        login(PASSWORD, 204);
    }

    @Test
    void rejectsDuplicateEmailIgnoringCaseAndDuplicateCpfIgnoringFormat() throws Exception {
        register();
        send("cadastro", REGISTRATION.replace(EMAIL, EMAIL.toUpperCase()).replace(CPF, "98765432100"), 409);
        send("cadastro", REGISTRATION.replace(EMAIL, "outro@example.test").replace(CPF, "12345678909"), 409);
    }

    @Test
    void rejectsInvalidRegistrationAndMissingCsrf() throws Exception {
        send("cadastro", REGISTRATION.replace("\"aceitouTermos\":true", "\"aceitouTermos\":false"), 400);
        send("cadastro", REGISTRATION.replace(CPF, "111.111.111-11"), 400);
        send("cadastro", REGISTRATION.replace("\"confirmarSenha\":\"" + PASSWORD + "\"", "\"confirmarSenha\":\"Diferente!42\""), 400);
        send("cadastro", REGISTRATION.replace(EMAIL, "invalido"), 400);
        mvc.perform(post("/api/auth/cadastro").contentType("application/json").content(REGISTRATION))
                .andExpect(status().isForbidden());
        assertThat(db.sql("SELECT COUNT(*) FROM usuario WHERE email = :email").param("email", EMAIL)
                .query(Long.class).single()).isZero();
    }

    @Test
    void describesInvalidNestedRegistrationFieldsInPortuguese() throws Exception {
        String independent = """
                {"conta":%s,"cnh":"123","categoriaCnh":"D","declarouConformidade":true}
                """.formatted(REGISTRATION.replace(CPF, "123"));
        var error = json.readTree(send("cadastro/autonomo", independent, 400)).get("detail").stringValue();
        assertThat(error).contains("CPF", "CNH").doesNotContain("conta.cpf");
        var malformed = json.readTree(send("cadastro", REGISTRATION.replace("1994-06-18", "18/06/1994"), 400));
        assertThat(malformed.get("detail").stringValue()).contains("formato inválido");
        assertThat(accounts.findAccount(EMAIL)).isEmpty();
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
    void throttlesRecoveryAcrossSessionsAndKeepsTheCurrentLinkDuringTheInterval() throws Exception {
        register();
        String recovery = "{\"email\":\"" + EMAIL + "\"}";
        send("recuperacao", recovery, 202);
        send("recuperacao", recovery, 202);
        assertThat(events.stream(RecoveryMail.class)).hasSize(1);

        db.sql("""
                UPDATE token_recuperacao
                SET expira_em = expira_em - INTERVAL '1 minute'
                WHERE id_usuario = (SELECT id FROM usuario WHERE email = :email)
                """).param("email", EMAIL).update();
        send("recuperacao", recovery, 202);

        var links = events.stream(RecoveryMail.class).toList();
        assertThat(links).hasSize(2);
        send("redefinicao", resetPayload(links.getFirst().token()), 400);
        send("redefinicao", resetPayload(links.getLast().token()), 204);
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
        return send(new MockHttpSession(), path, body, expected);
    }

    private String send(MockHttpSession session, String path, String body, int expected) throws Exception {
        Csrf csrf = csrf(session);
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
