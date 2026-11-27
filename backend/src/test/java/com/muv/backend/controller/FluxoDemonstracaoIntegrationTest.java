package com.muv.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.muv.backend.dto.DesignacaoFrete.ComandoDesignacao;
import com.muv.backend.dto.RespostaPrestador;
import com.muv.backend.dto.VehicleRequest;
import com.muv.backend.execucao.ExecucaoFreteService;
import com.muv.backend.execucao.RegrasExecucao.Acao;
import com.muv.backend.repository.ContratacaoRepository;
import com.muv.backend.repository.DesignacaoRepository;
import com.muv.backend.repository.PrestadorConsultaRepository;
import com.muv.backend.service.ContratacaoService;
import com.muv.backend.service.DesignacaoService;
import com.muv.backend.service.EnvioSolicitacaoService;
import com.muv.backend.service.EnvioSolicitacaoService.Endereco;
import com.muv.backend.service.EnvioSolicitacaoService.Envio;
import com.muv.backend.service.PropostaService;
import com.muv.backend.service.SolicitacoesDisponiveisService;
import com.muv.backend.service.VehicleService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "muv.demo.dispensar-verificacao=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/db/rf009-designacao.sql")
@Transactional
class FluxoDemonstracaoIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entidades;
    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private ObjectMapper json;
    @Autowired private VehicleService veiculos;
    @Autowired private PrestadorConsultaRepository prestadores;
    @Autowired private EnvioSolicitacaoService envio;
    @Autowired private SolicitacoesDisponiveisService solicitacoes;
    @Autowired private PropostaService propostas;
    @Autowired private ContratacaoRepository comparacao;
    @Autowired private ContratacaoService contratacao;
    @Autowired private DesignacaoRepository recursos;
    @Autowired private DesignacaoService designacao;
    @Autowired private ExecucaoFreteService execucao;

    @Test
    void usaSessaoRealDoGestorERestringeAOutraEmpresa() throws Exception {
        var perfil = "/api/prestadores/motoristas/perfil";
        mvc.perform(get(perfil)).andExpect(status().isUnauthorized());

        jdbc.update("UPDATE usuario SET senha = ? WHERE id = 909002", passwords.encode("SenhaTeste#2026"));
        var antesDoLogin = new MockHttpSession();
        var token = csrf(antesDoLogin);
        var login = mvc.perform(post("/api/auth/login").session(antesDoLogin)
                        .header(token.get("headerName").stringValue(), token.get("token").stringValue())
                        .param("email", "empresa909@example.test").param("senha", "SenhaTeste#2026"))
                .andExpect(status().isNoContent()).andReturn();
        var sessao = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get(perfil).session(sessao))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(909002));
        mvc.perform(get("/api/prestadores/909006/motoristas").session(sessao))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/prestadores/909002/motoristas").session(sessao).param("aptos", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == 909011)]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == 909012)]").isEmpty());

        jdbc.update("DELETE FROM vinculo_motorista WHERE prestador_id = 909002 AND motorista_id = 909018");
        entidades.clear();
        var aposLogin = csrf(sessao);
        mvc.perform(post("/api/prestadores/909002/motoristas/909018").session(sessao)
                        .header(aposLogin.get("headerName").stringValue(), aposLogin.get("token").stringValue()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.statusVerificacao").value("PENDENTE"));
        entidades.flush();
        assertThat(recursos.motoristaVerificado(909002, 909018)).isTrue();
    }

    private tools.jackson.databind.JsonNode csrf(MockHttpSession sessao) throws Exception {
        var resposta = mvc.perform(get("/api/auth/csrf").session(sessao))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(resposta.getResponse().getContentAsString());
    }

    @Test
    void percorreEnvioContratacaoEDesignacaoSemAprovacaoManual() {
        jdbc.update("DELETE FROM autorizacao_modalidade WHERE id_prestador = 909002 AND modalidade = 'PESSOA'");
        var carro = veiculos.create("empresa909@example.test",
                new VehicleRequest("Chevrolet", "Spin", 2024, "DMO1A23", "Prata", "passageiros",
                        BigDecimal.valueOf(6), null));
        assertThat(prestadores.buscar("PESSOA", null, null, null, 4))
                .anyMatch(p -> p.id() == 909002);

        var origem = new Endereco("60000-030", "Rua Origem", "30", "Centro", "Fortaleza", "CE");
        var destino = new Endereco("11000-031", "Rua Destino", "31", "Centro", "Santos", "SP");
        var valor = BigDecimal.valueOf(300);
        var condicoes = "Viagem de teste";
        var pedido = new Envio(UUID.randomUUID(), 909002, carro.id(), 4,
                        LocalDateTime.now(ZoneId.of("America/Fortaleza")).plusDays(1), null,
                        origem, destino, List.of(), valor, condicoes);
        var estimativa = new com.muv.backend.service.EstimativaFreteService.Estimativa(
                null, null, null, null, null, null, null, null, null, null, null, "Sem referência de teste.");
        var solicitacaoId = envio.enviar("cliente909@example.test", pedido, estimativa);
        assertThat(envio.enviar("cliente909@example.test", pedido, estimativa)).isEqualTo(solicitacaoId);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notificacao
                WHERE usuario_destino_id = 909002 AND solicitacao_id = ? AND titulo = 'Nova solicitação direcionada'
                """, Integer.class, solicitacaoId)).isEqualTo(1);

        assertThat(solicitacoes.listar(909002, "PESSOA", null, null, null))
                .anyMatch(s -> s.id() == solicitacaoId && s.autorizada());
        var propostaId = propostas.responder(solicitacaoId, 909002,
                new RespostaPrestador(RespostaPrestador.Tipo.MANTER, valor, condicoes,
                        null, null, UUID.randomUUID()));
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM notificacao
                WHERE usuario_destino_id = 909001 AND solicitacao_id = ? AND titulo = 'Nova proposta'
                """, Integer.class, solicitacaoId)).isEqualTo(1);
        assertThat(comparacao.propostas(solicitacaoId))
                .anyMatch(p -> p.id() == propostaId && p.autorizado());
        var freteId = contratacao.aceitar(solicitacaoId, propostaId, 909001).id();

        assertThat(recursos.motoristaVerificado(909002, 909011)).isTrue();
        assertThat(recursos.motoristaVerificado(909002, 909012)).isFalse();
        assertThat(recursos.motoristaVerificado(909002, 909013)).isFalse();
        assertThat(designacao.designar(freteId, 909002, new ComandoDesignacao(909011L, carro.id(), 0))
                .status()).isEqualTo("PRONTO_INICIO");
        assertThat(execucao.consultar(freteId, 909004, "Motorista Pendente").acoesElegiveis())
                .contains(Acao.INICIO);
        assertThat(jdbc.queryForObject("""
                SELECT status_verificacao FROM vinculo_motorista
                WHERE prestador_id = 909002 AND motorista_id = 909011
                """, String.class)).isEqualTo("PENDENTE");
    }
}
