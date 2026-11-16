package com.muv.backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record MotoristaCadastroRequest(
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Size(max = 20) @Pattern(regexp = "[0-9()+ .-]+") String telefone,
        @NotBlank @Pattern(regexp = "[0-9]{11}") String cnh,
        @NotBlank @Pattern(regexp = "A|B|AB|C|AC|D|AD|E|AE") String categoriaCnh,
        @NotBlank @Pattern(regexp = "[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}") String cpf,
        @NotNull @Past LocalDate nascimento,
        @NotBlank @Size(max = 64) String genero,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String senha,
        @NotBlank @Size(max = 72) String confirmarSenha,
        @Size(max = 1400000) String foto) {
    @Override public String toString() { return "MotoristaCadastroRequest[redacted]"; }
}
