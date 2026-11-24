package com.muv.backend.execucao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("rf010-test")
@Transactional
@Sql("/db/rf010-consulta.sql")
class ExecucaoFreteIntegrationTest {
    private static final String SCHEMA = "rf010_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void esquemaIsolado(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcClient db;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;

    private String execucao(long frete) {
        return "/api/fretes/" + frete + "/execucao";
    }

    @Test
    void consultaComSessaoRecursosEEstadoExato() throws Exception {
        mvc.perform(get(execucao(910061)).session(login("atual")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.freteId").value(910061))
                .andExpect(jsonPath("$.modalidade").value("PESSOA"))
                .andExpect(jsonPath("$.estado").value("PRONTO_INICIO"))
                .andExpect(jsonPath("$.usuarioAutenticado").value("Motorista Atual"))
                .andExpect(jsonPath("$.recursos.motorista").value("Motorista Atual"))
                .andExpect(jsonPath("$.recursos.placa").value("RF1A021"))
                .andExpect(jsonPath("$.recursos.telefone").doesNotExist())
                .andExpect(jsonPath("$.recursos.cnh").doesNotExist())
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.acoesElegiveis[0]").value("INICIO"));
        mvc.perform(get(execucao(910061)).session(login("empresa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acoesElegiveis").isEmpty());
    }

    @Test
    void separaInterrupcaoECicloDeOcorrenciasDoMesmoFrete() throws Exception {
        var prestador = login("empresa");
        mvc.perform(get(execucao(910062)).session(prestador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INTERROMPIDO"))
                .andExpect(jsonPath("$.revisaoDesignacao").value(2))
                .andExpect(jsonPath("$.interrupcaoId").value(910072))
                .andExpect(jsonPath("$.eventos[0].justificativa").value("Via bloqueada"))
                .andExpect(jsonPath("$.eventos[0].autor").value("Empresa Teste"))
                .andExpect(jsonPath("$.acoesElegiveis[0]").value("RETOMADA"))
                .andExpect(jsonPath("$.acoesElegiveis[1]").value("ENCERRAMENTO_SEM_CONCLUSAO"));
        mvc.perform(get(execucao(910063)).session(prestador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.eventos[0].tipo").value("OCORRENCIA"))
                .andExpect(jsonPath("$.cicloExecucaoId").value(910073))
                .andExpect(jsonPath("$.acoesElegiveis[0]").value("OCORRENCIA"))
                .andExpect(jsonPath("$.acoesElegiveis[1]").value("INTERRUPCAO"));
        assertThat(db.sql("SELECT count(*) FROM historico_execucao_frete").query(Long.class).single())
                .isEqualTo(4L);
        assertThat(db.sql("SELECT count(*) FROM notificacao").query(Long.class).single()).isZero();
    }

    @Test
    void recusaTerceirosEMotoristaSubstituido() throws Exception {
        mvc.perform(get(execucao(910062))).andExpect(status().isUnauthorized());
        for (String conta : List.of("cliente", "colega", "outra", "externo")) {
            mvc.perform(get(execucao(910062)).param("autorId", "910002").session(login(conta)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.eventos").doesNotExist());
        }
        var motoristaAnterior = login("atual");
        db.sql("UPDATE frete SET motorista_atual_id = 910012, revisao_designacao = 3 WHERE id = 910062").update();
        mvc.perform(get(execucao(910062)).session(motoristaAnterior)).andExpect(status().isForbidden());
        mvc.perform(get(execucao(910062)).session(login("colega"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.eventos[1].autor").value("Motorista Atual"));
        mvc.perform(get(execucao(999)).session(login("empresa"))).andExpect(status().isNotFound());
    }

    @Test
    void consultaRespeitaRecursosInelegiveisEDependenciaDoAutonomo() throws Exception {
        db.sql("UPDATE veiculo SET ativo = false WHERE id = 910021").update();
        mvc.perform(get(execucao(910061)).session(login("atual")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acoesElegiveis").isEmpty());
        mvc.perform(get(execucao(910064)).session(login("autonomo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("AGUARDANDO_DESIGNACAO"))
                .andExpect(jsonPath("$.acoesElegiveis").isEmpty());
    }

    @Test
    void consultaLegadoEmExecucaoSemEventoDeAbertura() throws Exception {
        db.sql("DELETE FROM historico_execucao_frete WHERE id = 910073").update();
        mvc.perform(get(execucao(910063)).session(login("empresa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.cicloExecucaoId").doesNotExist())
                .andExpect(jsonPath("$.acoesElegiveis").isEmpty())
                .andExpect(jsonPath("$.eventos[0].id").value(910074))
                .andExpect(jsonPath("$.eventos[0].horarioRegistro").doesNotExist());
    }

    @Test
    void designacaoDoAutonomoAindaBloqueiaOFluxoAntesDoInicio() throws Exception {
        var sessao = login("autonomo");
        var csrf = mvc.perform(get("/api/auth/csrf").session(sessao)).andExpect(status().isOk()).andReturn();
        var token = json.readTree(csrf.getResponse().getContentAsString());
        mvc.perform(post("/api/prestador/fretes/910064/designacao").session(sessao)
                        .header(token.get("headerName").asString(), token.get("token").asString())
                        .contentType("application/json")
                        .content("""
                                {"motoristaId":910011,"veiculoId":910021,"revisao":0}
                                """))
                .andExpect(status().is(422))
                .andExpect(result -> assertThat(result.getResponse().getErrorMessage())
                        .contains("A identidade de motorista do autônomo ainda não está associada"));
        mvc.perform(get(execucao(910064)).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("AGUARDANDO_DESIGNACAO"))
                .andExpect(jsonPath("$.acoesElegiveis").isEmpty());
    }

    private MockHttpSession login(String conta) throws Exception {
        String email = conta + "910@example.test";
        db.sql("UPDATE usuario SET senha = :senha WHERE email = :email")
                .param("senha", passwords.encode("Teste_RF010#"))
                .param("email", email)
                .update();
        var csrf = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = json.readTree(csrf.getResponse().getContentAsString());
        var session = (MockHttpSession) csrf.getRequest().getSession(false);
        var autenticado = mvc.perform(post("/api/auth/login").session(session)
                        .header(token.get("headerName").asString(), token.get("token").asString())
                        .param("email", email).param("senha", "Teste_RF010#"))
                .andExpect(status().isNoContent()).andReturn();
        return (MockHttpSession) autenticado.getRequest().getSession(false);
    }
}
