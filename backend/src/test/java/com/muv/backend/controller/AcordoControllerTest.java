package com.muv.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.muv.backend.dto.DesignacaoFrete.ComandoSubstituicao;
import com.muv.backend.service.DesignacaoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "selenium.browser=chrome")
@AutoConfigureMockMvc
@ActiveProfiles("rf001-local")
@Sql(scripts = {"/db/rf009-clean.sql", "/db/rf009-designacao.sql"})
@Sql(scripts = "/db/rf009-clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class AcordoControllerTest {
    private static final String ENDPOINT =
            "/api/cliente/fretes/{freteId}/acordos/{acordoId}/resposta";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private DesignacaoService service;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void clienteAceitaERepeticaoNaoAplicaNovamente() throws Exception {
        long acordo = criarAcordo();
        var cliente = sessao("cliente909@example.test");
        String csrf = csrf(cliente);

        mvc.perform(post(ENDPOINT, 909061L, acordo)
                        .session(cliente).header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceitar\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITA"))
                .andExpect(jsonPath("$.aplicado").value(true))
                .andExpect(jsonPath("$.revisaoFrete").value(2));
        mvc.perform(post(ENDPOINT, 909061L, acordo)
                        .session(cliente).header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceitar\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEITA"))
                .andExpect(jsonPath("$.aplicado").value(false))
                .andExpect(jsonPath("$.revisaoFrete").value(2));

        assertThat(jdbc.queryForMap("""
                SELECT valor_atual, condicoes_atuais, motorista_atual_id,
                       veiculo_atual_id, revisao_designacao
                FROM frete WHERE id = 909061
                """))
                .containsEntry("valor_atual", new BigDecimal("800.00"))
                .containsEntry("condicoes_atuais", "Nova condição de embarque")
                .containsEntry("motorista_atual_id", 909018L)
                .containsEntry("veiculo_atual_id", 909021L)
                .containsEntry("revisao_designacao", 2L);
        assertThat(jdbc.queryForMap("""
                SELECT tipo_evento, solicitacao_alteracao_id, motorista_id, veiculo_id
                FROM historico_execucao_frete
                WHERE frete_id = 909061 AND tipo_evento = 'SUBSTITUICAO_COM_ACORDO'
                """))
                .containsEntry("tipo_evento", "SUBSTITUICAO_COM_ACORDO")
                .containsEntry("solicitacao_alteracao_id", acordo)
                .containsEntry("motorista_id", 909018L)
                .containsEntry("veiculo_id", 909021L);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notificacao
                WHERE frete_id = 909061
                  AND ((usuario_destino_id = 909001
                        AND titulo = 'Nova proposta de alteração do frete')
                    OR (usuario_destino_id = 909002
                        AND titulo = 'Alteração aceita'))
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForMap("""
                SELECT status, data_hora_resposta IS NOT NULL AS respondido
                FROM solicitacao_alteracao_acordo WHERE id = ?
                """, acordo))
                .containsEntry("status", "ACEITA")
                .containsEntry("respondido", true);
    }

    @Test
    void clienteRejeitaSemAlterarFrete() throws Exception {
        long acordo = criarAcordo();
        var cliente = sessao("cliente909@example.test");

        mvc.perform(post(ENDPOINT, 909061L, acordo)
                        .session(cliente).header("X-CSRF-TOKEN", csrf(cliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceitar\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADA"));
        assertThat(jdbc.queryForMap("""
                SELECT valor_atual, motorista_atual_id, veiculo_atual_id, revisao_designacao
                FROM frete WHERE id = 909061
                """))
                .containsEntry("valor_atual", new BigDecimal("740.00"))
                .containsEntry("motorista_atual_id", 909010L)
                .containsEntry("veiculo_atual_id", 909024L)
                .containsEntry("revisao_designacao", 1L);
    }

    @Test
    void usuarioAlheioNaoResponde() throws Exception {
        long acordo = criarAcordo();
        var alheio = sessao("outra909@example.test");

        mvc.perform(post(ENDPOINT, 909061L, acordo)
                        .session(alheio).header("X-CSRF-TOKEN", csrf(alheio))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceitar\":true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void recursoInelegivelAntesDoAceiteMantemAcordoPendente() throws Exception {
        long acordo = criarAcordo();
        jdbc.update("UPDATE veiculo SET ativo = false WHERE id = 909021");
        var cliente = sessao("cliente909@example.test");

        mvc.perform(post(ENDPOINT, 909061L, acordo)
                        .session(cliente).header("X-CSRF-TOKEN", csrf(cliente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceitar\":true}"))
                .andExpect(status().isUnprocessableContent());
        assertThat(jdbc.queryForObject(
                "SELECT status FROM solicitacao_alteracao_acordo WHERE id = ?",
                String.class, acordo)).isEqualTo("PENDENTE");
    }

    private long criarAcordo() {
        return service.substituir(909061L, 909002L,
                new ComandoSubstituicao(909018L, 909021L, "Nova composição", 1L,
                        new BigDecimal("800.00"), "Nova condição de embarque"))
                .solicitacaoAcordoId();
    }

    private MockHttpSession sessao(String email) {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));
        var sessao = new MockHttpSession();
        sessao.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contexto);
        return sessao;
    }

    private String csrf(MockHttpSession sessao) throws Exception {
        var resposta = mvc.perform(get("/api/auth/csrf").session(sessao))
                .andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(resposta.getContentAsString()).get("token").stringValue();
    }
}
