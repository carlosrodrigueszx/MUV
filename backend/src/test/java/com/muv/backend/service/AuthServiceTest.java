package com.muv.backend.service;

import com.muv.backend.dto.AuthRequests.Registration;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {
    private final AuthService service = new AuthService(null, null, null, event -> {}, Clock.systemUTC());

    @Test
    void rejectsAnInvalidCpfBeforeProcessingAccountData() {
        assertThatThrownBy(() -> service.register(registration("123.456.789-00", "SenhaTeste!", "SenhaTeste!")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void rejectsPasswordsBeyondBcryptByteLimitEvenWhenCharacterCountIsValid() {
        String password = "ã".repeat(37);
        assertThatThrownBy(() -> service.register(registration("123.456.789-09", password, password)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
    }

    private static Registration registration(String cpf, String password, String confirmation) {
        return new Registration("Passageiro de Teste", cpf, "85900000000", "teste@example.test",
                "Prefiro não informar", LocalDate.of(1994, 6, 18), password, confirmation);
    }
}
