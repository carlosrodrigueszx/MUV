package com.muv.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.muv.backend.testutil.PrestadorConsultaFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TripProviderSearchIntegrationTest {
    private static final String CONSULTA = "/api/prestadores/consulta";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;

    @BeforeEach
    void seed() {
        PrestadorConsultaFixtures.seed(jdbc);
    }

    @Test
    void returnsOnlyActiveAuthorizedCompatibleVehiclesWithEnoughSeats() throws Exception {
        // A different approved modality on the same provider must not leak into the result.
        jdbc.update("""
                INSERT INTO autorizacao_modalidade (id, modalidade, status, id_prestador)
                VALUES (991021, 'ENCOMENDA', 'APROVADO', 991001)
                """);
        jdbc.update("UPDATE veiculo SET id_prestador = 991001 WHERE id = 991003");
        jdbc.update("""
                INSERT INTO veiculo (id, modelo, ano, cor, placa, capacidade_passageiros,
                    capacidade_carga_kg, marca, modalidade, ativo, id_prestador)
                VALUES (991021, 'Van pequena', 2024, 'Branco', 'TST-1121', 10, 0, 'MUV', 'PESSOA', TRUE, 991001)
                """);

        mvc.perform(get(CONSULTA).param("modalidade", "PESSOA").param("quantidadePassageiros", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(991001))
                .andExpect(jsonPath("$[0].veiculosDisponiveis.length()").value(1))
                .andExpect(jsonPath("$[0].veiculosDisponiveis[0].id").value(991001));
        mvc.perform(get(CONSULTA).param("quantidadePassageiros", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].veiculosDisponiveis[0].capacidadePassageiros").value(20));
        mvc.perform(get(CONSULTA).param("quantidadePassageiros", "21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsInvalidPassengerCountsAndCargoModality() throws Exception {
        for (String invalid : new String[]{"0", "-1", "1.5"}) {
            mvc.perform(get(CONSULTA).param("quantidadePassageiros", invalid))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get(CONSULTA).param("quantidadePassageiros", "1").param("modalidade", "ENCOMENDA"))
                .andExpect(status().isBadRequest());
    }
}
