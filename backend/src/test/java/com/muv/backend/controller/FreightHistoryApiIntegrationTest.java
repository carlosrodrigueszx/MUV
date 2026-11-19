package com.muv.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("rf001-local")
@Transactional
@Sql("/db/rf014-freight-history.sql")
class FreightHistoryApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsFilteredHistoryWithProposalsAndTimeline() throws Exception {
        mvc.perform(get("/api/historico-fretes")
                        .header("X-User-Id", 914001L)
                        .param("perfil", "CLIENTE")
                        .param("modalidade", "Pessoas")
                        .param("situacao", "Concluído")
                        .param("periodo", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("FR-914301"))
                .andExpect(jsonPath("$[0].modality").value("Pessoas"))
                .andExpect(jsonPath("$[0].origin").value("Campinas"))
                .andExpect(jsonPath("$[0].destination").value("Sorocaba"))
                .andExpect(jsonPath("$[0].status").value("Concluído"))
                .andExpect(jsonPath("$[0].proposals.length()").value(1))
                .andExpect(jsonPath("$[0].proposals[0].id").value("P-914203"))
                .andExpect(jsonPath("$[0].timeline.length()").value(2))
                .andExpect(jsonPath("$[0].timeline[0].title").value("Entrega concluída"));
    }

    @Test
    void enforcesAccountScopeAtHttpBoundary() throws Exception {
        mvc.perform(get("/api/historico-fretes")
                        .header("X-User-Id", 914004L)
                        .param("perfil", "CLIENTE")
                        .param("periodo", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("SL-914103"))
                .andExpect(jsonPath("$[0].request.requestedBy").value("Outro Cliente RF014"));
    }

    @Test
    void rejectsMissingAccountAndInvalidFilters() throws Exception {
        mvc.perform(get("/api/historico-fretes")
                        .param("perfil", "CLIENTE"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/historico-fretes")
                        .header("X-User-Id", 914001L)
                        .param("perfil", "CLIENTE")
                        .param("modalidade", "Mudança"))
                .andExpect(status().isBadRequest());
    }
}
