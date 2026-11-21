package com.muv.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RespostaPrestador(Tipo tipo, BigDecimal valorRevisado, String condicoesRevisadas,
        BigDecimal novoValor, String justificativa, UUID chaveEnvio) {
    public enum Tipo { MANTER, AJUSTAR }
}
