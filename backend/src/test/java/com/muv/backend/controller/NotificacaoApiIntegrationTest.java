package com.muv.backend.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("rf001-local")
@Transactional
@Sql("/db/rf014-freight-history.sql")
class NotificacaoApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @Test
    void listaSomenteDoDestinatarioComContextoAutorizado() throws Exception {
        inserir(914001, "Proposta recebida", "Você recebeu uma proposta.", null, 914101L, false,
                LocalDateTime.of(2026, 11, 20, 10, 0));
        inserir(914002, "Contratação confirmada", "O cliente aceitou sua proposta.", 914301L, 914102L, false,
                LocalDateTime.of(2026, 11, 20, 11, 0));

        mvc.perform(get("/api/notificacoes").session(sessao("cliente-rf014@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].titulo").value("Proposta recebida"))
                .andExpect(jsonPath("$.itens[0].lida").value(false))
                .andExpect(jsonPath("$.itens[0].solicitacaoId").value(914101))
                .andExpect(jsonPath("$.itens[0].acaoUrl").value("/cliente/solicitacoes/914101/propostas"))
                .andExpect(jsonPath("$.naoLidas").value(1))
                .andExpect(jsonPath("$.itens[0].email").doesNotExist())
                .andExpect(jsonPath("$.itens[0].cpf").doesNotExist());

        mvc.perform(get("/api/notificacoes").session(sessao("prestador-alfa-rf014@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].freteId").value(914301))
                .andExpect(jsonPath("$.itens[0].acaoUrl").value("/prestador/historico?frete=914301"));

        mvc.perform(get("/api/notificacoes")).andExpect(status().isUnauthorized());
    }

    @Test
    void marcaUmaOuTodasSomenteParaContaAutenticada() throws Exception {
        long clienteNaoLida = inserir(914001, "Proposta recebida", "Nova proposta.", null, 914101L, false,
                LocalDateTime.of(2026, 11, 20, 10, 0));
        inserir(914001, "Frete concluído", "O frete foi concluído.", 914301L, 914102L, false,
                LocalDateTime.of(2026, 11, 20, 11, 0));
        long prestadorNaoLida = inserir(914002, "Contratação confirmada", "Frete contratado.", 914301,
                914102L, false, LocalDateTime.of(2026, 11, 20, 12, 0));

        var session = sessao("cliente-rf014@example.test");
        var csrf = csrf(session);
        mvc.perform(patch("/api/notificacoes/{id}/leitura", prestadorNaoLida)
                        .session(session).header(csrf.header, csrf.token))
                .andExpect(status().isNoContent());
        assertEquals(0, readFlag(prestadorNaoLida));
        mvc.perform(patch("/api/notificacoes/{id}/leitura", clienteNaoLida).session(session))
                .andExpect(status().isForbidden());
        assertEquals(1, readFlag(clienteNaoLida));

        mvc.perform(patch("/api/notificacoes/{id}/leitura", clienteNaoLida)
                        .session(session).header(csrf.header, csrf.token))
                .andExpect(status().isNoContent());
        assertEquals(1, unreadCount(914001));
        assertEquals(0, readFlag(clienteNaoLida));
        assertEquals(0, readFlag(prestadorNaoLida));

        mvc.perform(patch("/api/notificacoes/leitura").session(session).header(csrf.header, csrf.token))
                .andExpect(status().isNoContent());
        assertEquals(0, unreadCount(914001));
        assertEquals(0, readFlag(prestadorNaoLida));

        mvc.perform(get("/api/notificacoes").session(session).param("filtro", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    private long inserir(long destinatario, String titulo, String mensagem, Long frete, Long solicitacao,
                         boolean lida, LocalDateTime criadoEm) {
        return jdbc.queryForObject("""
                INSERT INTO notificacao (titulo, mensagem, usuario_destino_id, frete_id, solicitacao_id,
                                         lida, criado_em)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, titulo, mensagem, destinatario, frete, solicitacao, lida, criadoEm);
    }

    private long unreadCount(long usuarioId) {
        return jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_destino_id=? AND lida=false",
                Long.class, usuarioId);
    }

    private int readFlag(long notificacaoId) {
        return jdbc.queryForObject("SELECT CASE WHEN lida THEN 1 ELSE 0 END FROM notificacao WHERE id=?",
                Integer.class, notificacaoId);
    }

    private record Csrf(String header, String token) {}

    private Csrf csrf(MockHttpSession session) throws Exception {
        var result = mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return new Csrf(body.get("headerName").asText(), body.get("token").asText());
    }

    private MockHttpSession sessao(String email) {
        var authentication = new UsernamePasswordAuthenticationToken(email, null,
                List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
