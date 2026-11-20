package com.muv.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record VehicleRequest(
        @NotBlank @Size(max = 64) String brand,
        @NotBlank @Size(max = 32) String model,
        @Min(1900) @Max(2100) int year,
        @NotBlank @Size(max = 8) @Pattern(regexp = "(?i)[A-Z]{3}-?[0-9][A-Z0-9][0-9]{2}") String plate,
        @NotBlank @Size(max = 32) String color,
        @NotBlank @Pattern(regexp = "passageiros|carga") String type,
        @NotNull @DecimalMin("1.0") @Digits(integer = 8, fraction = 2) BigDecimal capacity,
        @Size(max = 255) String description) {
}
