package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.muv.backend.dto.PrestadorConsultaResponse;
import com.muv.backend.testutil.PrestadorConsultaFixtures;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PrestadorConsultaIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PrestadorConsultaService service;

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
}
