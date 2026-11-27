package com.muv.backend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RespostaPrestador(Tipo tipo, BigDecimal valorRevisado, String condicoesRevisadas,
        BigDecimal novoValor, String justificativa, UUID chaveEnvio, String versaoEstimativa) {
    public RespostaPrestador(Tipo tipo, BigDecimal valorRevisado, String condicoesRevisadas,
            BigDecimal novoValor, String justificativa, UUID chaveEnvio) {
        this(tipo, valorRevisado, condicoesRevisadas, novoValor, justificativa, chaveEnvio, null);
    }

    public enum Tipo { MANTER, AJUSTAR }
}
