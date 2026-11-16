package com.muv.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MotoristaVerificacaoRequest(
        @NotBlank @Pattern(regexp = "PENDENTE|VERIFICADO|REJEITADO") String statusVerificacao) {}
