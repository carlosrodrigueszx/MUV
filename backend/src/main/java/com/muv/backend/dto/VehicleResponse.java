package com.muv.backend.dto;

import java.math.BigDecimal;

public record VehicleResponse(
        long id,
        String type,
        String brand,
        String model,
        int year,
        String plate,
        String color,
        BigDecimal capacity,
        String description,
        String status) {
}
