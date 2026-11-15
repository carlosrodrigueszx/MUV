package com.muv.backend.controller;

import com.muv.backend.dto.FreightHistoryProfile;
import com.muv.backend.service.FreightHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreightHistoryControllerTest {

    @Mock
    private FreightHistoryService service;

    @Test
    void delegatesAuthenticatedRequestToService() {
        var controller = new FreightHistoryController(service);
        var month = YearMonth.of(2026, 11);
        when(service.list(10L, FreightHistoryProfile.CLIENTE, "Encomendas", "Concluído", month))
                .thenReturn(List.of());

        var response = controller.list(
                10L,
                FreightHistoryProfile.CLIENTE,
                "Encomendas",
                "Concluído",
                month);

        assertEquals(List.of(), response);
        verify(service).list(10L, FreightHistoryProfile.CLIENTE, "Encomendas", "Concluído", month);
    }

    @Test
    void convertsInvalidFilterToBadRequest() {
        var controller = new FreightHistoryController(service);
        when(service.list(10L, FreightHistoryProfile.CLIENTE, "Inválida", null, null))
                .thenThrow(new IllegalArgumentException("Modalidade inválida."));

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.list(10L, FreightHistoryProfile.CLIENTE, "Inválida", null, null));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void rejectsRequestWithoutAuthenticatedAccount() {
        var controller = new FreightHistoryController(service);

        var exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.list(null, FreightHistoryProfile.CLIENTE, null, null, null));

        assertEquals(401, exception.getStatusCode().value());
    }
}
