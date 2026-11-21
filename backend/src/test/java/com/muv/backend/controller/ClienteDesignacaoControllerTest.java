package com.muv.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "selenium.browser=chrome")
@AutoConfigureMockMvc
@ActiveProfiles("rf001-local")
@Sql(scripts = {"/db/rf009-clean.sql", "/db/rf009-designacao.sql"})
@Sql(scripts = "/db/rf009-clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ClienteDesignacaoControllerTest {
    private static final String ENDPOINT = "/api/cliente/fretes/{id}/designacao";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void clienteConsultaDesignacaoAtualComContratoMinimoEPrivado() throws Exception {
        var resposta = mvc.perform(get(ENDPOINT, 909061L)
                        .session(sessao("cliente909@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freteId").value(909061))
                .andExpect(jsonPath("$.motoristaNome").value("Motorista Verificado"))
                .andExpect(jsonPath("$.veiculoMarca").value("Chevrolet"))
                .andExpect(jsonPath("$.veiculoModelo").value("Spin quatro lugares"))
                .andExpect(jsonPath("$.veiculoPlaca").value("RF9A024"))
                .andReturn().getResponse().getContentAsString();

        var campos = StreamSupport.stream(json.readTree(resposta).propertyNames().spliterator(), false)
                .collect(Collectors.toSet());
        assertThat(campos).isEqualTo(Set.of(
                "freteId", "status", "motoristaNome",
                "veiculoMarca", "veiculoModelo", "veiculoPlaca"));
        assertThat(resposta.toLowerCase()).doesNotContain(
                "cpf", "cnh", "telefone", "verificacao", "senha", "criptograf");
    }

    @Test
    void clienteConsultaFreteAindaSemDesignacao() throws Exception {
        mvc.perform(get(ENDPOINT, 909060L)
                        .session(sessao("cliente909@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_DESIGNACAO"))
                .andExpect(jsonPath("$.motoristaNome").doesNotExist())
                .andExpect(jsonPath("$.veiculoPlaca").doesNotExist());
    }

    @Test
    void clienteAlheioNaoConsulta() throws Exception {
        mvc.perform(get(ENDPOINT, 909061L).session(sessao("outra909@example.test")))
                .andExpect(status().isNotFound());
    }

    @Test
    void sessaoAusenteRecebe401() throws Exception {
        mvc.perform(get(ENDPOINT, 909061L)).andExpect(status().isUnauthorized());
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
}
