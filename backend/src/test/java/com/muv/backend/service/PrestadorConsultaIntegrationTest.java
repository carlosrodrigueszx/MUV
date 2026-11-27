package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.muv.backend.dto.PrestadorConsultaResponse;
import com.muv.backend.testutil.PrestadorConsultaFixtures;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PrestadorConsultaIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PrestadorConsultaService service;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @BeforeEach
    void seedDatabase() {
        PrestadorConsultaFixtures.seed(jdbc);
    }

    @AfterEach
    void cleanDatabase() {
        PrestadorConsultaFixtures.clean(jdbc);
    }

    @Test
    void filtersByPersonModalityAndReturnsOnlyApprovedCompatibleActiveVehicles() {
        List<PrestadorConsultaResponse> results = service.buscar("PESSOA", null, null, null);

        assertThat(results).extracting(PrestadorConsultaResponse::id).containsExactly(991001L);
        assertThat(results.getFirst().nome()).isEqualTo("Autônomo Horizonte Teste");
        assertThat(results.getFirst().tipo()).isEqualTo("AUTONOMO");
        assertThat(results.getFirst().modalidadesAutorizadas()).containsExactly("PESSOA");
        assertThat(results.getFirst().regioesAtendidas())
                .containsExactly(new PrestadorConsultaResponse.Regiao("Fortaleza", "CE"));
        assertThat(results.getFirst().veiculosDisponiveis())
                .extracting(PrestadorConsultaResponse.Veiculo::modelo)
                .containsExactly("Micro-ônibus Ativo Teste");
    }

    @Test
    void filtersByEncomendaModalityAndRegionAndReturnsCompanyInformation() {
        List<PrestadorConsultaResponse> results = service.buscar("ENCOMENDA", "Recife", "PE", "EMPRESA");

        assertThat(results).hasSize(1);
        PrestadorConsultaResponse provider = results.getFirst();
        assertThat(provider.id()).isEqualTo(991003L);
        assertThat(provider.nome()).isEqualTo("Cooperativa Rota Azul Fictícia");
        assertThat(provider.tipo()).isEqualTo("EMPRESA");
        assertThat(provider.modalidadesAutorizadas()).containsExactly("ENCOMENDA");
        assertThat(provider.regioesAtendidas())
                .containsExactly(new PrestadorConsultaResponse.Regiao("Recife", "PE"));
        assertThat(provider.veiculosDisponiveis())
                .containsExactly(new PrestadorConsultaResponse.Veiculo(
                        991003L, "MUV", "Furgão Cooperativa Teste", 2025, "ENCOMENDA", 0,
                        new java.math.BigDecimal("850.00")));
    }

    @Test
    void filtersByRegionEvenWhenModalityIsNotSelected() {
        List<PrestadorConsultaResponse> results = service.buscar(null, "Fortaleza", "CE", null);

        assertThat(results).extracting(PrestadorConsultaResponse::id).containsExactly(991001L);
    }

    @Test
    void returnsNoProvidersWhenNoApprovedProviderMatchesTheRegion() {
        assertThat(service.buscar("PESSOA", "Recife", "PE", null)).isEmpty();
    }

    @Test
    void prioritizesDirectCoverageAndDeduplicatesProvidersWithSeveralRegions() {
        jdbc.update("UPDATE autorizacao_modalidade SET status = 'APROVADO' WHERE id = 991002");
        jdbc.update("UPDATE regiao_atuacao SET cidade = 'Caucaia' WHERE id = 991002");
        jdbc.update("INSERT INTO regiao_atuacao (id, cidade, estado, id_prestador) VALUES (991020, 'Caucaia', 'CE', 991001)");
        jdbc.update("""
                INSERT INTO autorizacao_modalidade (id, modalidade, status, id_prestador)
                VALUES (991021, 'ENCOMENDA', 'APROVADO', 991001)
                """);
        jdbc.update("""
                INSERT INTO veiculo (id, modelo, ano, cor, placa, capacidade_passageiros,
                    capacidade_carga_kg, marca, modalidade, ativo, id_prestador)
                VALUES (991021, 'Furgão', 2024, 'Branco', 'TST-1121', 0, 800, 'MUV', 'ENCOMENDA', TRUE, 991001)
                """);

        var results = service.buscar("PESSOA", null, null, null, null, 2304400);

        assertThat(results).extracting(PrestadorConsultaResponse::id).containsExactly(991001L, 991002L);
        assertThat(results).extracting(PrestadorConsultaResponse::cobertura).containsExactly(
                new PrestadorConsultaResponse.Cobertura("DIRETA",
                        new PrestadorConsultaResponse.Municipio(2304400, "Fortaleza", "CE")),
                new PrestadorConsultaResponse.Cobertura("PROXIMA",
                        new PrestadorConsultaResponse.Municipio(2303709, "Caucaia", "CE")));
        assertThat(service.buscar("PESSOA", null, null, null, 13, 2304400))
                .extracting(PrestadorConsultaResponse::id).containsExactly(991001L);
        assertThat(results.getFirst().veiculosDisponiveis()).extracting(PrestadorConsultaResponse.Veiculo::id)
                .containsExactly(991001L);
        jdbc.update("DELETE FROM veiculo WHERE id = 991021");
        jdbc.update("DELETE FROM autorizacao_modalidade WHERE id = 991021");
        jdbc.update("DELETE FROM regiao_atuacao WHERE id = 991020");
    }

    @Test
    void tiesUseProviderIdAndResultsBeyondConfiguredRadiusAreExcluded() {
        jdbc.update("UPDATE autorizacao_modalidade SET status = 'APROVADO' WHERE id = 991002");
        jdbc.update("UPDATE regiao_atuacao SET cidade = 'Caucaia' WHERE id IN (991001, 991002)");
        jdbc.update("UPDATE municipio SET latitude = 0, longitude = 0 WHERE codigo_ibge = 2304400");
        jdbc.update("UPDATE municipio SET latitude = 0, longitude = degrees(49.99 / 6371.0088) WHERE codigo_ibge = 2303709");

        assertThat(service.buscar("PESSOA", null, null, null, null, 2304400))
                .extracting(PrestadorConsultaResponse::id).containsExactly(991001L, 991002L);

        jdbc.update("UPDATE municipio SET longitude = degrees(50.01 / 6371.0088) WHERE codigo_ibge = 2303709");
        assertThat(service.buscar("PESSOA", null, null, null, null, 2304400)).isEmpty();
    }

    @Test
    void searchesAcrossStateBordersAndRejectsUnknownMunicipalities() {
        jdbc.update("UPDATE regiao_atuacao SET cidade = 'Timon', estado = 'MA' WHERE id = 991003");

        var results = service.buscar("ENCOMENDA", null, null, null, null, 2211001);

        assertThat(results).extracting(PrestadorConsultaResponse::id).containsExactly(991003L);
        assertThat(results.getFirst().cobertura()).isEqualTo(new PrestadorConsultaResponse.Cobertura(
                "PROXIMA", new PrestadorConsultaResponse.Municipio(2112209, "Timon", "MA")));
        assertThat(service.buscar("ENCOMENDA", null, null, null, null, 2304400)).isEmpty();
        assertThatThrownBy(
                () -> service.buscar("ENCOMENDA", null, null, null, null, 9999999))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void enviaEncomendaAoPrestadorSelecionadoComReenvioIdempotente() throws Exception {
        long cliente = 991099;
        String email = "cliente-rf005@example.test";
        jdbc.update("INSERT INTO usuario (id, nome, telefone, email, senha) VALUES (?, 'Cliente', '85999999999', ?, 'teste')",
                cliente, email);
        jdbc.update("INSERT INTO passageiro (id, cpf) VALUES (?, '99109900000')", cliente);
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(email, null, List.of()));
        var sessao = new MockHttpSession();
        sessao.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contexto);
        var csrf = json.readTree(mvc.perform(get("/api/auth/csrf").session(sessao))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String cabecalho = csrf.get("headerName").stringValue();
        String token = csrf.get("token").stringValue();
        String rota = "/api/cliente/solicitacoes/encomendas";
        String endereco = """
                {"cep":"60000000","logradouro":"Rua Teste","numero":"10","bairro":"Centro","cidade":"Recife","estado":"PE"}
                """;
        String corpo = """
                {"chave":"%s","prestadorId":991003,"partida":"%s","origem":%s,"destino":%s,
                "descricaoVolumes":"2 caixas","pesoKg":25,"alturaCm":50,"larguraCm":40,"profundidadeCm":30,
                "coleta":{"nome":"Coleta","telefone":"85999999999"},
                "entrega":{"nome":"Entrega","telefone":"85999999999"}}
                """.formatted(java.util.UUID.randomUUID(),
                java.time.LocalDateTime.now().plusDays(1).withNano(0), endereco, endereco);
        mvc.perform(post(rota).session(sessao).header(cabecalho, token).contentType("application/json")
                        .content(corpo.replace("991003", "991001")))
                .andExpect(status().isConflict());
        mvc.perform(post(rota).session(sessao).header(cabecalho, token).contentType("application/json").content(corpo))
                .andExpect(status().isCreated());
        Long id = jdbc.queryForObject(
                "SELECT id FROM solicitacao_frete WHERE id_passageiro = ? AND id_prestador = 991003 AND modalidade = 'ENCOMENDA'",
                Long.class, cliente);
        mvc.perform(post(rota).session(sessao).header(cabecalho, token).contentType("application/json").content(corpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
        assertThat(jdbc.queryForObject("SELECT descricao_volumes FROM solicitacao_encomenda WHERE id = ?",
                String.class, id)).isEqualTo("2 caixas");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE solicitacao_id = ? AND usuario_destino_id = 991003",
                Long.class, id)).isEqualTo(1);
        jdbc.update("DELETE FROM notificacao WHERE solicitacao_id = ?", id);
        jdbc.update("DELETE FROM solicitacao_encomenda WHERE id = ?", id);
        jdbc.update("DELETE FROM solicitacao_frete WHERE id = ?", id);
    }

    @Test
    void exposesCanonicalMunicipalitiesAndValidatesRegionalFilters() throws Exception {
        String route = "/api/prestadores/consulta";
        mvc.perform(get(route + "/municipios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == 2304400)].cidade").value("Fortaleza"))
                .andExpect(jsonPath("$[?(@.codigo == 2304400)].estado").value("CE"));
        mvc.perform(get(route).param("municipioCodigo", "2304400").param("modalidade", "PESSOA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cobertura.nivel").value("DIRETA"));
        mvc.perform(get(route).param("municipioCodigo", "2304400").param("modalidade", "ENCOMENDA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get(route).param("municipioCodigo", "9999999").param("modalidade", "PESSOA"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(route).param("municipioCodigo", "2304400"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(route).param("municipioCodigo", "2304400").param("modalidade", "INVALIDA"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(route).param("cidade", "Fortaleza").param("estado", "CE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].cobertura").doesNotExist());
    }
}
