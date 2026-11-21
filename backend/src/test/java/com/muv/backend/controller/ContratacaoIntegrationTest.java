package com.muv.backend.controller;

import com.muv.backend.dto.Contratacao.Aceite;
import com.muv.backend.dto.RespostaPrestador;
import com.muv.backend.service.ContratacaoService;
import com.muv.backend.service.PropostaService;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/db/rf007-solicitacoes.sql")
@Sql(scripts = {"/db/rf008-clean.sql", "/db/rf007-clean.sql"},
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ContratacaoIntegrationTest {
    private static final long CLIENTE = 907001;
    private static final long PESSOA = 907031;
    private static final long ENCOMENDA = 907032;
    private static final long AUTONOMO = 907002;
    private static final long EMPRESA = 907003;
    private static final String CONSULTA = "/api/cliente/solicitacoes/{id}/propostas";

    @Autowired ContratacaoService contratacao;
    @Autowired PropostaService propostas;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void titularConsultaAmbasModalidadesEConfirmaComCsrf() throws Exception {
        var cliente = sessao("cliente907@example.test");
        var outro = sessao("autor907@example.test");
        mvc.perform(get(CONSULTA, ENCOMENDA)).andExpect(status().isUnauthorized());
        mvc.perform(get(CONSULTA, PESSOA).session(outro)).andExpect(status().isNotFound());
        mvc.perform(get(CONSULTA, ENCOMENDA).session(cliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.propostas").isEmpty())
                .andExpect(jsonPath("$.solicitacao.modalidade").value("ENCOMENDA"));
        long proposta = enviar(PESSOA, AUTONOMO);
        mvc.perform(get(CONSULTA, PESSOA).session(cliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.solicitacao.modalidade").value("PESSOA"))
                .andExpect(jsonPath("$.propostas[0].valor").value(315.78))
                .andExpect(jsonPath("$.propostas[0].tipoPrestador").value("AUTONOMO"));
        String aceite = CONSULTA + "/{proposta}/aceite";
        mvc.perform(post(aceite, PESSOA, proposta).session(cliente)).andExpect(status().isForbidden());
        mvc.perform(post(aceite, PESSOA, proposta).session(outro).header("X-CSRF-TOKEN", csrf(outro))).andExpect(status().isNotFound());
        mvc.perform(post(aceite, ENCOMENDA, proposta).session(cliente).header("X-CSRF-TOKEN", csrf(cliente))).andExpect(status().isNotFound());
        mvc.perform(post(aceite, PESSOA, proposta).session(cliente).header("X-CSRF-TOKEN", csrf(cliente))).andExpect(status().isCreated());
        long frete = contratacao.comparar(PESSOA, CLIENTE).freteId();
        mvc.perform(post(aceite, PESSOA, proposta).session(cliente).header("X-CSRF-TOKEN", csrf(cliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(frete));
        mvc.perform(get("/api/cliente/fretes/{id}", frete).session(outro)).andExpect(status().isNotFound());
        mvc.perform(get("/api/cliente/fretes/{id}", frete).session(cliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.frete.status").value("AGUARDANDO_DESIGNACAO"))
                .andExpect(jsonPath("$.frete.condicoes").value("Inclui pedágios"));
        assertEquals("CONTRATADA", contratacao.comparar(PESSOA, CLIENTE).solicitacao().status());
        assertEquals(AUTONOMO, jdbc.queryForObject(
                "SELECT usuario_destino_id FROM notificacao WHERE frete_id = ?", Long.class, frete));
    }

    @Test
    void doisAceitesConcorrentesGeramUmFrete() throws Exception {
        long primeira = enviar(PESSOA, AUTONOMO);
        long segunda = enviar(PESSOA, EMPRESA);
        concorrer(() -> confirmarOuConflito(primeira), () -> confirmarOuConflito(segunda));
        assertEquals(1, quantidade("frete", "solicitacao_id", PESSOA));
        assertEquals(1, quantidade("notificacao", "frete_id", contratacao.comparar(PESSOA, CLIENTE).freteId()));
        var estados = contratacao.comparar(PESSOA, CLIENTE).propostas().stream().map(p -> p.status()).sorted().toList();
        assertEquals(java.util.List.of("ACEITA", "ENVIADA"), estados);
    }

    @Test
    void aceiteConcorreComEnvioDoRf007() throws Exception {
        long proposta = enviar(PESSOA, AUTONOMO);
        var nova = resposta(PESSOA, EMPRESA);
        concorrer(() -> contratacao.aceitar(PESSOA, proposta, CLIENTE), () -> {
            try {
                return propostas.responder(PESSOA, EMPRESA, nova);
            } catch (ResponseStatusException conflito) {
                assertEquals(409, conflito.getStatusCode().value());
                return null;
            }
        });
        assertEquals(1, quantidade("frete", "solicitacao_id", PESSOA));
        assertEquals(1, quantidade("notificacao", "frete_id", contratacao.comparar(PESSOA, CLIENTE).freteId()));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> propostas.responder(PESSOA, EMPRESA, resposta(PESSOA, EMPRESA))).getStatusCode().value());
    }

    @Test
    void desfazAceiteSeNotificacaoFalhar() {
        long proposta = enviar(ENCOMENDA, AUTONOMO);
        jdbc.execute("ALTER TABLE notificacao ADD CONSTRAINT rf008_falha_teste CHECK (frete_id IS NULL)");
        try {
            assertThrows(DataAccessException.class, () -> contratacao.aceitar(ENCOMENDA, proposta, CLIENTE));
            assertEquals(0, quantidade("frete", "solicitacao_id", ENCOMENDA));
            var comparacao = contratacao.comparar(ENCOMENDA, CLIENTE);
            assertEquals("ABERTA", comparacao.solicitacao().status());
            assertEquals("ENVIADA", comparacao.propostas().getFirst().status());
        } finally {
            jdbc.execute("ALTER TABLE notificacao DROP CONSTRAINT rf008_falha_teste");
        }
    }

    private MockHttpSession sessao(String email) {
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new UsernamePasswordAuthenticationToken(email, null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));
        var sessao = new MockHttpSession();
        sessao.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contexto);
        return sessao;
    }

    private String csrf(MockHttpSession sessao) throws Exception {
        var resposta = mvc.perform(get("/api/auth/csrf").session(sessao))
                .andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(resposta.getContentAsString()).get("token").stringValue();
    }

    private Aceite confirmarOuConflito(long proposta) {
        try {
            return contratacao.aceitar(PESSOA, proposta, CLIENTE);
        } catch (ResponseStatusException conflito) {
            assertEquals(409, conflito.getStatusCode().value());
            return null;
        }
    }

    private void concorrer(Callable<?> primeira, Callable<?> segunda) throws Exception {
        var barreira = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { barreira.await(5, TimeUnit.SECONDS); return primeira.call(); });
            var b = executor.submit(() -> { barreira.await(5, TimeUnit.SECONDS); return segunda.call(); });
            a.get(10, TimeUnit.SECONDS);
            b.get(10, TimeUnit.SECONDS);
        }
    }

    private long enviar(long solicitacao, long prestador) {
        return propostas.responder(solicitacao, prestador, resposta(solicitacao, prestador));
    }

    private RespostaPrestador resposta(long solicitacao, long prestador) {
        var oferta = propostas.avaliar(solicitacao, prestador);
        return new RespostaPrestador(RespostaPrestador.Tipo.MANTER, oferta.valorCliente(),
                oferta.condicoesCliente(), null, null, UUID.randomUUID());
    }

    private int quantidade(String tabela, String coluna, long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela + " WHERE " + coluna + " = ?", Integer.class, id);
    }
}
