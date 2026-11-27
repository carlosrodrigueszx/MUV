package com.muv.backend.repository;

import com.muv.backend.dto.VehicleRequest;
import com.muv.backend.dto.VehicleResponse;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class VehicleRepository {
    private static final RowMapper<VehicleResponse> VEHICLE_ROW = VehicleRepository::mapVehicle;
    private final JdbcClient db;

    public VehicleRepository(JdbcClient db) {
        this.db = db;
    }

    public List<VehicleResponse> findAllByProvider(long providerId) {
        return db.sql("""
                SELECT id, modalidade, marca, modelo, ano, placa, cor,
                       capacidade_passageiros, capacidade_carga_kg, descricao, ativo
                FROM veiculo WHERE id_prestador = :providerId
                ORDER BY marca, modelo, id
                """).param("providerId", providerId).query(VEHICLE_ROW).list();
    }

    public Optional<VehicleResponse> findByIdAndProvider(long id, long providerId) {
        return db.sql("""
                SELECT id, modalidade, marca, modelo, ano, placa, cor,
                       capacidade_passageiros, capacidade_carga_kg, descricao, ativo
                FROM veiculo WHERE id = :id AND id_prestador = :providerId
                """).param("id", id).param("providerId", providerId)
                .query(VEHICLE_ROW).optional();
    }

    public Optional<String> findModalityByIdAndProvider(long id, long providerId) {
        return db.sql("SELECT modalidade FROM veiculo WHERE id = :id AND id_prestador = :providerId")
                .param("id", id).param("providerId", providerId)
                .query(String.class).optional();
    }

    public VehicleResponse insert(long providerId, VehicleRequest vehicle, String plate,
            String modality, int passengerCapacity, BigDecimal cargoCapacity) {
        long id = db.sql("""
                INSERT INTO veiculo
                    (modelo, ano, cor, placa, capacidade_passageiros, capacidade_carga_kg,
                     marca, modalidade, ativo, id_prestador, descricao)
                VALUES (:model, :year, :color, :plate, :passengerCapacity, :cargoCapacity,
                        :brand, :modality, TRUE, :providerId, :description)
                RETURNING id
                """).param("model", vehicle.model().trim()).param("year", vehicle.year())
                .param("color", vehicle.color().trim()).param("plate", plate)
                .param("passengerCapacity", passengerCapacity).param("cargoCapacity", cargoCapacity)
                .param("brand", vehicle.brand().trim()).param("modality", modality)
                .param("providerId", providerId).param("description", cleanDescription(vehicle.description()))
                .query(Long.class).single();
        return findByIdAndProvider(id, providerId).orElseThrow();
    }

    public boolean update(long id, long providerId, VehicleRequest vehicle, String plate,
            String modality, int passengerCapacity, BigDecimal cargoCapacity) {
        int updated = db.sql("""
                UPDATE veiculo SET modelo = :model, ano = :year, cor = :color,
                    placa = :plate,
                    capacidade_passageiros = CASE WHEN :modality = 'AMBOS'
                        THEN capacidade_passageiros ELSE :passengerCapacity END,
                    capacidade_carga_kg = :cargoCapacity, marca = :brand,
                    modalidade = :modality, descricao = :description
                WHERE id = :id AND id_prestador = :providerId
                """).param("model", vehicle.model().trim()).param("year", vehicle.year())
                .param("color", vehicle.color().trim()).param("plate", plate)
                .param("passengerCapacity", passengerCapacity).param("cargoCapacity", cargoCapacity)
                .param("brand", vehicle.brand().trim()).param("modality", modality)
                .param("description", cleanDescription(vehicle.description()))
                .param("id", id).param("providerId", providerId).update();
        return updated == 1;
    }

    private static VehicleResponse mapVehicle(ResultSet rs, int row) throws SQLException {
        String modality = rs.getString("modalidade");
        boolean passengers = "PESSOA".equals(modality);
        BigDecimal capacity = passengers
                ? BigDecimal.valueOf(rs.getInt("capacidade_passageiros"))
                : rs.getBigDecimal("capacidade_carga_kg");
        return new VehicleResponse(rs.getLong("id"), passengers ? "passageiros" : "carga",
                rs.getString("marca"), rs.getString("modelo"), rs.getInt("ano"),
                rs.getString("placa"), rs.getString("cor"), capacity,
                rs.getString("descricao"), rs.getBoolean("ativo") ? "ativo" : "inativo");
    }

    private static String cleanDescription(String description) {
        return description == null ? "" : description.trim();
    }
}
