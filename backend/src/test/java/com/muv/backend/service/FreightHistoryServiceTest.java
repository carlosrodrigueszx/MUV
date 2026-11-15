package com.muv.backend.service;

import com.muv.backend.dto.FreightHistoryProfile;
import com.muv.backend.repository.FreightHistoryQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreightHistoryServiceTest {

    @Mock
    private FreightHistoryQueryRepository repository;

    private FreightHistoryService service;

    @BeforeEach
    void setUp() {
        service = new FreightHistoryService(repository);
    }

    @Test
    void listsOnlyRecordsReturnedForAuthenticatedClient() {
        var month = YearMonth.of(2026, 11);
        var row = new FreightHistoryQueryRepository.HistoryRow(
                86L,
                3086L,
                "ENCOMENDA",
                "São Paulo",
                "Santos",
                LocalDateTime.of(2026, 11, 20, 11, 15),
                LocalDateTime.of(2026, 11, 13, 9, 0),
                "Ana Oliveira",
                "12 caixas",
                new BigDecimal("480.00"),
                "Entrega até 12:00.",
                "CONCLUIDO");
        var proposal = new FreightHistoryQueryRepository.ProposalRow(
                9021L, "Litoral Express", new BigDecimal("480.00"), "ACEITA");
        var event = new FreightHistoryQueryRepository.TimelineRow(
                LocalDateTime.of(2026, 11, 20, 11, 15),
                "CONCLUSAO",
                "Rafael Souza",
                "Recebido por Juliana.",
                null);

        when(repository.findHistory(10L, FreightHistoryProfile.CLIENTE, "ENCOMENDA", "CONCLUIDO", month))
                .thenReturn(List.of(row));
        when(repository.findProposals(86L, 10L, FreightHistoryProfile.CLIENTE))
                .thenReturn(List.of(proposal));
        when(repository.findTimeline(3086L)).thenReturn(List.of(event));

        var result = service.list(10L, FreightHistoryProfile.CLIENTE, "Encomendas", "Concluído", month);

        assertEquals(1, result.size());
        assertEquals("FR-3086", result.getFirst().id());
        assertEquals("R$ 480,00", result.getFirst().agreedValue());
        assertEquals("Concluído", result.getFirst().status());
        assertEquals(1, result.getFirst().proposals().size());
        assertEquals(1, result.getFirst().timeline().size());
        verify(repository).findHistory(10L, FreightHistoryProfile.CLIENTE, "ENCOMENDA", "CONCLUIDO", month);
    }

    @Test
    void providerProfileIsPassedToRepositoryForAccessRestriction() {
        when(repository.findHistory(20L, FreightHistoryProfile.PRESTADOR, null, null, null))
                .thenReturn(List.of());

        service.list(20L, FreightHistoryProfile.PRESTADOR, "todas", "todas", null);

        verify(repository).findHistory(20L, FreightHistoryProfile.PRESTADOR, null, null, null);
    }

    @Test
    void normalizesContractedFilterWithoutNarrowingItsActiveStatuses() {
        when(repository.findHistory(20L, FreightHistoryProfile.PRESTADOR, null, "CONTRATADO", null))
                .thenReturn(List.of());

        service.list(20L, FreightHistoryProfile.PRESTADOR, null, "Contratado", null);

        verify(repository).findHistory(20L, FreightHistoryProfile.PRESTADOR, null, "CONTRATADO", null);
    }

    @Test
    void rejectsMissingAuthenticatedAccount() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.list(null, FreightHistoryProfile.CLIENTE, null, null, null));

        assertEquals("A conta autenticada é obrigatória.", exception.getMessage());
    }

    @Test
    void rejectsInvalidFiltersBeforeQueryingDatabase() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.list(10L, FreightHistoryProfile.CLIENTE, "Mudança", null, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.list(10L, FreightHistoryProfile.CLIENTE, null, "Desconhecido", null));
    }
}
