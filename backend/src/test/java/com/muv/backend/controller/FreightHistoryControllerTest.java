package com.muv.backend.controller;

import com.muv.backend.dto.FreightHistoryProfile;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.FreightHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreightHistoryControllerTest {

    @Mock
    private FreightHistoryService service;

    @Mock
    private AuthRepository accounts;

    @Test
    void delegatesAuthenticatedRequestToService() {
        var controller = new FreightHistoryController(service, accounts);
        var month = YearMonth.of(2026, 11);
        var principal = (java.security.Principal) () -> "cliente@example.test";
        when(accounts.findAccount("cliente@example.test"))
                .thenReturn(Optional.of(new AuthRepository.Account(
                        10L, "Cliente", "cliente@example.test", "senha")));
        when(service.list(10L, FreightHistoryProfile.CLIENTE, "Encomendas", "Concluído", month))
                .thenReturn(List.of());

        var response = controller.list(
                principal,
                FreightHistoryProfile.CLIENTE,
                "Encomendas",
                "Concluído",
                month);

        assertEquals(List.of(), response);
        verify(service).list(10L, FreightHistoryProfile.CLIENTE, "Encomendas", "Concluído", month);
    }

    @Test
    void convertsInvalidFilterToBadRequest() {
        var controller = new FreightHistoryController(service, accounts);
        var principal = (java.security.Principal) () -> "cliente@example.test";
        when(accounts.findAccount("cliente@example.test"))
                .thenReturn(Optional.of(new AuthRepository.Account(
                        10L, "Cliente", "cliente@example.test", "senha")));
        when(service.list(10L, FreightHistoryProfile.CLIENTE, "Inválida", null, null))
                .thenThrow(new IllegalArgumentException("Modalidade inválida."));

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.list(principal, FreightHistoryProfile.CLIENTE, "Inválida", null, null));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void rejectsRequestWithoutAuthenticatedAccount() {
        var controller = new FreightHistoryController(service, accounts);

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.list(null, FreightHistoryProfile.CLIENTE, null, null, null));

        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    void rejectsProfileNotOwnedByAuthenticatedAccount() {
        var controller = new FreightHistoryController(service, accounts);
        var principal = (java.security.Principal) () -> "cliente@example.test";
        when(accounts.findAccount("cliente@example.test"))
                .thenReturn(Optional.of(new AuthRepository.Account(
                        10L, "Cliente", "cliente@example.test", "senha")));
        when(service.list(10L, FreightHistoryProfile.PRESTADOR, null, null, null))
                .thenThrow(new AccessDeniedException("A conta não possui o perfil solicitado."));

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.list(principal, FreightHistoryProfile.PRESTADOR, null, null, null));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void returnsProfilesOwnedByAuthenticatedAccount() {
        var controller = new FreightHistoryController(service, accounts);
        var principal = (java.security.Principal) () -> "cliente@example.test";
        when(accounts.findAccount("cliente@example.test"))
                .thenReturn(Optional.of(new AuthRepository.Account(
                        10L, "Cliente", "cliente@example.test", "senha")));
        when(service.availableProfiles(10L)).thenReturn(List.of(FreightHistoryProfile.CLIENTE));

        var profiles = controller.profiles(principal);

        assertEquals(List.of(FreightHistoryProfile.CLIENTE), profiles);
    }
}
