package com.muv.backend.dto;

import java.time.LocalDateTime;

public record AchadoPerdidoResponse(
        long id,
        String descricao,
        LocalDateTime dataEncontrado,
        String status,
        long veiculoId,
        String veiculoMarca,
        String veiculoModelo,
        String veiculoPlaca,
        Long freteId,
        String foto) {}
