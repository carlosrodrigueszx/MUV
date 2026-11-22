package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.muv.backend.dto.RegistrarAchadoRequest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
class AchadoPerdidoIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AchadoPerdidoService service;

    private long providerId;
    private long vehicleId;
    private String photo;
    private final List<Long> providerIds = new ArrayList<>();
    private final List<Long> vehicleIds = new ArrayList<>();
    private final List<Long> accountIds = new ArrayList<>();
    private final List<Long> driverIds = new ArrayList<>();

    @BeforeEach
    void prepare() throws Exception {
        providerId = provider("rf015-" + UUID.randomUUID() + "@example.test");
        vehicleId = vehicle(providerId, "RF015" + String.format("%02d", Math.floorMod(providerId, 100)));
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        photo = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    @AfterEach
    void cleanup() {
        for (long id : vehicleIds) {
            jdbc.update("DELETE FROM foto_item_perdido WHERE id_item IN (SELECT id FROM achado_perdido WHERE id_veiculo=?)", id);
            jdbc.update("DELETE FROM achado_perdido WHERE id_veiculo=?", id);
        }
        for (long id : providerIds) jdbc.update("DELETE FROM veiculo WHERE id_prestador=?", id);
        for (long id : providerIds) jdbc.update("DELETE FROM vinculo_motorista WHERE prestador_id=?", id);
        for (long id : driverIds) jdbc.update("DELETE FROM motorista WHERE id=?", id);
        for (long id : providerIds) jdbc.update("DELETE FROM prestador WHERE id=?", id);
        for (long id : accountIds) jdbc.update("DELETE FROM usuario WHERE id=?", id);
        providerIds.clear(); vehicleIds.clear(); accountIds.clear(); driverIds.clear();
    }

    @Test
    void registersListsAndConfirmsItemWithPhoto() {
        long id = service.registrar(providerId, request(vehicleId, null));

        assertThat(jdbc.queryForObject("SELECT status FROM achado_perdido WHERE id=?", String.class, id))
                .isEqualTo("EM_GUARDA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM foto_item_perdido WHERE id_item=? AND conteudo IS NOT NULL", Integer.class, id))
                .isEqualTo(1);
        var listed = service.listar(providerId);
        assertThat(listed).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(id);
            assertThat(item.foto()).startsWith("data:image/png;base64,");
            assertThat(item.freteId()).isNull();
        });

        service.confirmarEntrega(providerId, id);
        assertThat(service.listar(providerId).getFirst().status()).isEqualTo("DEVOLVIDO");
    }

    @Test
    void keepsProvidersIsolatedForListingAndUpdates() {
        long ownId = service.registrar(providerId, request(vehicleId, null));
        long otherProvider = provider("rf015-other-" + UUID.randomUUID() + "@example.test");
        long otherVehicle = vehicle(otherProvider, "RF015" + String.format("%02d", Math.floorMod(otherProvider, 100)));
        long otherId = service.registrar(otherProvider, request(otherVehicle, null));

        assertThat(service.listar(providerId)).extracting("id").containsExactly(ownId);
        var denied = assertThrows(ResponseStatusException.class,
                () -> service.confirmarEntrega(providerId, otherId));
        assertThat(denied.getStatusCode().value()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT status FROM achado_perdido WHERE id=?", String.class, otherId))
                .isEqualTo("EM_GUARDA");
    }

    @Test
    void linkedDriverCanRegisterUsingTheProvidersVehicle() {
        long driverAccount = jdbc.queryForObject("""
                INSERT INTO usuario (nome,telefone,email,senha,is_admin)
                VALUES ('Motorista RF015','85999990001',?,'hash',false) RETURNING id
                """, Long.class, "rf015-driver-" + UUID.randomUUID() + "@example.test");
        long driverId = jdbc.queryForObject("""
                INSERT INTO motorista (nome,telefone,cnh,usuario_id)
                VALUES ('Motorista RF015','85999990001',?,?) RETURNING id
                """, Long.class, "RF" + String.format("%018d", Math.floorMod(driverAccount, 1_000_000_000_000_000_000L)), driverAccount);
        accountIds.add(driverAccount);
        driverIds.add(driverId);
        jdbc.update("INSERT INTO vinculo_motorista (prestador_id,motorista_id,status_verificacao) VALUES (?,?,'VERIFICADO')",
                providerId, driverId);

        long itemId = service.registrar(driverAccount, request(vehicleId, null));

        assertThat(jdbc.queryForObject("SELECT id_registrador FROM achado_perdido WHERE id=?", Long.class, itemId))
                .isEqualTo(driverAccount);
        assertThat(service.listar(driverAccount)).extracting("id").containsExactly(itemId);
    }

    @Test
    void rollsBackItemWhenPhotoPersistenceFails() {
        String constraint = "rf015_photo_reject_" + UUID.randomUUID().toString().replace("-", "");
        // NOT VALID deixa fotos preexistentes do ambiente de teste intactas, mas a
        // restrição continua sendo aplicada às novas inserções do cenário.
        jdbc.execute("ALTER TABLE foto_item_perdido ADD CONSTRAINT " + constraint
                + " CHECK (tipo <> 'image/png') NOT VALID");
        try {
            assertThrows(DataAccessException.class, () -> service.registrar(providerId, request(vehicleId, null)));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM achado_perdido WHERE id_veiculo=?", Integer.class, vehicleId))
                    .isZero();
        } finally {
            jdbc.execute("ALTER TABLE foto_item_perdido DROP CONSTRAINT " + constraint);
        }
    }

    private RegistrarAchadoRequest request(long vehicle, Long freight) {
        return new RegistrarAchadoRequest("Mochila encontrada", LocalDateTime.parse("2026-10-01T10:30:00"),
                vehicle, freight, photo);
    }

    private long provider(String email) {
        long id = jdbc.queryForObject("""
                INSERT INTO usuario (nome,telefone,email,senha,is_admin)
                VALUES ('Prestador RF015','85999990000',?,'hash',false) RETURNING id
                """, Long.class, email);
        jdbc.update("INSERT INTO prestador (id) VALUES (?)", id);
        providerIds.add(id);
        accountIds.add(id);
        return id;
    }

    private long vehicle(long owner, String plate) {
        long id = jdbc.queryForObject("""
                INSERT INTO veiculo (modelo,ano,cor,placa,capacidade_passageiros,capacidade_carga_kg,
                    marca,modalidade,id_prestador)
                VALUES ('Van',2024,'Branca',?,16,0,'MUV','PESSOA',?) RETURNING id
                """, Long.class, plate, owner);
        vehicleIds.add(id);
        return id;
    }
}
