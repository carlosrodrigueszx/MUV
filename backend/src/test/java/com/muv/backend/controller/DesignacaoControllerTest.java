package com.muv.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "selenium.browser=chrome")
@AutoConfigureMockMvc
@ActiveProfiles("rf001-local")
@Sql(scripts = {"/db/rf009-clean.sql", "/db/rf009-designacao.sql"})
@Sql(scripts = "/db/rf009-clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DesignacaoControllerTest {
    private static final String ENDPOINT = "/api/prestador/fretes/{id}/designacao";
    private static final String SUBSTITUICOES = "/api/prestador/fretes/{id}/substituicoes";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void exigeSessaoAutenticada() throws Exception {
        mvc.perform(get(ENDPOINT, 909060L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void prestadorResponsavelConsultaContextoECompatibilidade() throws Exception {
        mvc.perform(get(ENDPOINT, 909060L).session(sessao("empresa909@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freteId").value(909060L))
                .andExpect(jsonPath("$.revisao").value(0))
                .andExpect(jsonPath("$.motoristas.length()").value(2))
                .andExpect(jsonPath("$.motoristas[0].id").value(909010L))
                .andExpect(jsonPath("$.veiculos[?(@.id == 909020)].compativel").value(true))
                .andExpect(jsonPath("$.veiculos[?(@.id == 909023)].incompatibilidades[0]")
                        .value("CAPACIDADE_CARGA_INSUFICIENTE"));
    }

    @Test
    void ocultaFreteDeOutroPrestadorEFreteInexistente() throws Exception {
        var outroPrestador = sessao("outra909@example.test");
        mvc.perform(get(ENDPOINT, 909060L).session(outroPrestador))
                .andExpect(status().isNotFound());
        mvc.perform(get(ENDPOINT, 999999L).session(sessao("empresa909@example.test")))
                .andExpect(status().isNotFound());
    }

    @Test
    void respostaNaoExpoeDadosPessoaisOuCifrados() throws Exception {
        mvc.perform(get(ENDPOINT, 909061L).session(sessao("empresa909@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..cpf").doesNotExist())
                .andExpect(jsonPath("$..cnh").doesNotExist())
                .andExpect(jsonPath("$..telefone").doesNotExist())
                .andExpect(jsonPath("$..senha").doesNotExist())
                .andExpect(jsonPath("$..habilitacaoCifrada").doesNotExist())
                .andExpect(jsonPath("$..statusVerificacao").doesNotExist());
    }

    @Test
    void prestadorOrganizacionalConfirmaDesignacao() throws Exception {
        var sessao = sessao("empresa909@example.test");

        mvc.perform(post(ENDPOINT, 909060L)
                        .session(sessao)
                        .header("X-CSRF-TOKEN", csrf(sessao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motoristaId":909010,"veiculoId":909020,"revisao":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRONTO_INICIO"))
                .andExpect(jsonPath("$.revisao").value(1))
                .andExpect(jsonPath("$.designacaoAtual.motoristaId").value(909010L))
                .andExpect(jsonPath("$.designacaoAtual.veiculoId").value(909020L));
    }

    @Test
    void rejeitaRecursosAusentesEAcessoAlheio() throws Exception {
        var responsavel = sessao("empresa909@example.test");
        mvc.perform(post(ENDPOINT, 909060L)
                        .session(responsavel)
                        .header("X-CSRF-TOKEN", csrf(responsavel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"veiculoId\":909020,\"revisao\":0}"))
                .andExpect(status().isBadRequest());

        var outro = sessao("outra909@example.test");
        mvc.perform(post(ENDPOINT, 909060L)
                        .session(outro)
                        .header("X-CSRF-TOKEN", csrf(outro))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motoristaId":909013,"veiculoId":909025,"revisao":0}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejeitaEstadoInelegivelERevisaoDesatualizada() throws Exception {
        var sessao = sessao("empresa909@example.test");
        String csrf = csrf(sessao);
        mvc.perform(post(ENDPOINT, 909061L)
                        .session(sessao)
                        .header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motoristaId":909010,"veiculoId":909024,"revisao":1}
                                """))
                .andExpect(status().isConflict());
        mvc.perform(post(ENDPOINT, 909060L)
                        .session(sessao)
                        .header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motoristaId":909010,"veiculoId":909020,"revisao":9}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void autonomoSemMotoristaAssociadoFalhaSemQualquerEscrita() throws Exception {
        var autonomo = sessao("autonomo909@example.test");

        mvc.perform(post(ENDPOINT, 909066L)
                        .session(autonomo)
                        .header("X-CSRF-TOKEN", csrf(autonomo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"veiculoId\":909026,\"revisao\":0}"))
                .andExpect(status().isUnprocessableContent());

        var frete = jdbc.queryForMap("""
                SELECT status, motorista_atual_id, veiculo_atual_id, revisao_designacao
                FROM frete WHERE id = 909066
                """);
        org.assertj.core.api.Assertions.assertThat(frete)
                .containsEntry("status", "AGUARDANDO_DESIGNACAO")
                .containsEntry("motorista_atual_id", null)
                .containsEntry("veiculo_atual_id", null)
                .containsEntry("revisao_designacao", 0L);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM motorista WHERE usuario_id = 909008",
                Integer.class)).isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM vinculo_motorista WHERE prestador_id = 909008",
                Integer.class)).isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM historico_execucao_frete WHERE frete_id = 909066",
                Integer.class)).isZero();
    }

    @Test
    void substituiUmRecursoOuOsDois() throws Exception {
        var sessao = sessao("empresa909@example.test");
        mvc.perform(post(SUBSTITUICOES, 909061L)
                        .session(sessao)
                        .header("X-CSRF-TOKEN", csrf(sessao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"veiculoId":909021,"justificativa":"Manutenção","revisao":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.designacaoAtual.motoristaId").value(909010L))
                .andExpect(jsonPath("$.designacaoAtual.veiculoId").value(909021L));

        mvc.perform(post(SUBSTITUICOES, 909061L)
                        .session(sessao)
                        .header("X-CSRF-TOKEN", csrf(sessao))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motoristaId":909018,"veiculoId":909024,
                                 "justificativa":"Nova escala","revisao":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.designacaoAtual.motoristaId").value(909018L))
                .andExpect(jsonPath("$.designacaoAtual.veiculoId").value(909024L));
    }

    @Test
    void rejeitaSubstituicaoIncompativelEConfirmacaoComRevisaoAntiga() throws Exception {
        var sessao = sessao("empresa909@example.test");
        String csrf = csrf(sessao);
        mvc.perform(post(SUBSTITUICOES, 909061L)
                        .session(sessao)
                        .header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"veiculoId":909023,"justificativa":"Teste","revisao":1}
                                """))
                .andExpect(status().isUnprocessableContent());

        mvc.perform(post(SUBSTITUICOES, 909061L)
                        .session(sessao)
                        .header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"veiculoId":909021,"justificativa":"Primeira","revisao":1}
                                """))
                .andExpect(status().isOk());
        mvc.perform(post(SUBSTITUICOES, 909061L)
                        .session(sessao)
                        .header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motoristaId":909018,"justificativa":"Segunda","revisao":1}
                                """))
                .andExpect(status().isConflict());
    }

    private MockHttpSession sessao(String email) {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken(
                email,
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));
        var sessao = new MockHttpSession();
        sessao.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                contexto);
        return sessao;
    }

    private String csrf(MockHttpSession sessao) throws Exception {
        var resposta = mvc.perform(get("/api/auth/csrf").session(sessao))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        return json.readTree(resposta.getContentAsString()).get("token").stringValue();
    }
}
