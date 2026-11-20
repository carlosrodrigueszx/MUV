package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.muv.backend.dto.VehicleRequest;
import com.muv.backend.dto.VehicleResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VehicleServiceIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private VehicleService service;
    private String email;

    @BeforeEach
    void createProvider() {
        email = "rf004-" + java.util.UUID.randomUUID() + "@example.test";
        Long id = jdbc.queryForObject("""
                INSERT INTO usuario (nome, telefone, email, senha, is_admin)
                VALUES ('Prestador RF004', '85999990000', ?, 'hash-de-teste', false)
                RETURNING id
                """, Long.class, email);
        jdbc.update("INSERT INTO prestador (id) VALUES (?)", id);
    }

    @Test
    void persistsAndRecoversPassengerVehicleAndUpdatesIt() {
        VehicleRequest request = request("passageiros", "ABC1D23", "16", "Van executiva");

        VehicleResponse saved = service.create(email, request);
        assertThat(saved.id()).isPositive();
        assertThat(saved.type()).isEqualTo("passageiros");
        assertThat(saved.capacity()).isEqualByComparingTo("16");
        assertThat(jdbc.queryForObject("SELECT capacidade_passageiros FROM veiculo WHERE id = ?",
                Integer.class, saved.id())).isEqualTo(16);

        VehicleResponse recovered = service.get(email, saved.id());
        assertThat(recovered.plate()).isEqualTo("ABC1D23");
        assertThat(service.list(email)).extracting(VehicleResponse::id).contains(saved.id());

        service.update(email, saved.id(), request("passageiros", "ABC1D23", "18", "Van atualizada"));
        assertThat(jdbc.queryForObject("SELECT descricao FROM veiculo WHERE id = ?", String.class, saved.id()))
                .isEqualTo("Van atualizada");
        assertThat(service.get(email, saved.id()).capacity()).isEqualByComparingTo("18");
    }

    @Test
    void persistsCargoCapacityAndRejectsDuplicatePlate() {
        VehicleResponse saved = service.create(email,
                request("carga", "DEF4G56", "3500.50", "Caminhão"));

        assertThat(saved.type()).isEqualTo("carga");
        assertThat(jdbc.queryForObject("SELECT capacidade_carga_kg FROM veiculo WHERE id = ?",
                BigDecimal.class, saved.id())).isEqualByComparingTo("3500.50");
        assertThatThrownBy(() -> service.create(email,
                request("carga", "DEF4G56", "4000", "Outro veículo")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("placa");
    }

    @Test
    void vehicleIsInvisibleToAnotherProvider() {
        VehicleResponse saved = service.create(email,
                request("passageiros", "GHI7J89", "12", "Veículo privado"));
        String otherEmail = "rf004-outro-" + java.util.UUID.randomUUID() + "@example.test";
        Long otherId = jdbc.queryForObject("""
                INSERT INTO usuario (nome, telefone, email, senha, is_admin)
                VALUES ('Outro prestador', '85999990001', ?, 'hash-de-teste', false)
                RETURNING id
                """, Long.class, otherEmail);
        jdbc.update("INSERT INTO prestador (id) VALUES (?)", otherId);

        assertThat(service.list(otherEmail)).isEmpty();
        assertThatThrownBy(() -> service.get(otherEmail, saved.id()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Veículo não encontrado");
    }

    private static VehicleRequest request(String type, String plate, String capacity, String description) {
        return new VehicleRequest("Marca", "Modelo", 2024, plate, "Branco", type,
                new BigDecimal(capacity), description);
    }
}
