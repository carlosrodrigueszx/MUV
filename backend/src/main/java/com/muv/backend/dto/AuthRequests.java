package com.muv.backend.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class AuthRequests {
    private AuthRequests() {}

    public record Registration(
            @NotBlank @Size(max = 255) String nome,
            @NotBlank @Pattern(regexp = "[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}") String cpf,
            @NotBlank @Size(max = 20) @Pattern(regexp = "[0-9()+ .-]+") String telefone,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 64) String genero,
            @NotNull @Past LocalDate nascimento,
            @NotBlank @Size(min = 8, max = 72) String senha,
            @NotBlank @Size(max = 72) String confirmarSenha) {}

    public record Recovery(@NotBlank @Email @Size(max = 255) String email) {}

    public record Reset(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
            @NotBlank @Size(min = 8, max = 72) String senha,
            @NotBlank @Size(max = 72) String confirmarSenha) {}
}
