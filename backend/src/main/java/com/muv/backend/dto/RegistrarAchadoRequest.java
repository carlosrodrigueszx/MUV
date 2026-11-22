package com.muv.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record RegistrarAchadoRequest(
        @NotBlank @Size(max = 255) String descricao,
        @NotNull LocalDateTime dataEncontrado,
        @NotNull @Positive Long veiculoId,
        @Positive Long freteId,
        @NotBlank @Size(max = 1_400_000) String foto) {
    @Override public String toString() { return "RegistrarAchadoRequest[redacted]"; }
}
