package com.muv.backend.service;

import com.muv.backend.repository.FreightCancellationRepository;
import com.muv.backend.repository.FreightCancellationRepository.Freight;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FreightCancellationRulesTest {
    private final FreightCancellationRepository repository = mock(FreightCancellationRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final FreightCancellationService service = new FreightCancellationService(repository,
            new NotificacaoInternaService(publisher));

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u00a0", "\u2003", "\u202f"})
    void rejectsBlankReason(String reason) {
        rejects(400, () -> service.cancel(42, 7, reason));
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AGUARDANDO_DESIGNACAO", "PRONTO_INICIO"})
    void providerCancelsAndNotifiesOnlyClient(String state) {
        freight(state);
        var cancelledAtUtc = LocalDateTime.of(2026, 11, 19, 17, 30);
        when(repository.markCancelled(42)).thenReturn(cancelledAtUtc);
        service.cancel(42, 9, "  Mudança de planos  ");
        verify(repository).markCancelled(42);
        verify(repository).addCancellationEvent(42, 9, "Mudança de planos", cancelledAtUtc);
        verify(publisher).publishEvent(new NotificacaoInternaSolicitada(7, "Frete cancelado",
                "O frete FR-42 foi cancelado. Consulte o motivo no registro do cancelamento.",
                42L, null, cancelledAtUtc));
    }

    @ParameterizedTest
    @ValueSource(strings = {"EM_EXECUCAO", "INTERROMPIDO", "CONCLUIDO", "ENCERRADO_SEM_CONCLUSAO", "CANCELADO"})
    void rejectsStartedOrAlreadyCancelledFreight(String state) {
        freight(state);
        rejects(409, () -> service.cancel(42, 7, "Motivo"));
        verify(repository).findForUpdate(42);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsUnrelatedAccount() {
        freight("PRONTO_INICIO");
        rejects(403, () -> service.cancel(42, 99, "Motivo"));
        verify(repository).findForUpdate(42);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsMissingFreight() {
        rejects(404, () -> service.cancel(42, 7, "Motivo"));
        verify(repository).findForUpdate(42);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(publisher);
    }

    private void freight(String state) {
        when(repository.findForUpdate(42)).thenReturn(Optional.of(new Freight(42, 7, 9, state)));
    }

    private void rejects(int status, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                error -> org.assertj.core.api.Assertions.assertThat(error.getStatusCode().value()).isEqualTo(status));
    }
}
