package com.muv.backend.service;

import com.muv.backend.repository.FreightCancellationRepository;
import com.muv.backend.repository.FreightCancellationRepository.Freight;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FreightCancellationServiceTest {

    @Test
    void clienteCancelaFreteAntesDoInicioENotificaSomenteOPrestador() {
        var repository = mock(FreightCancellationRepository.class);
        var service = new FreightCancellationService(repository);

        long freteId = 42L;
        long clienteId = 7L;
        long prestadorId = 9L;
        String justificativa = "Mudança de planos";
        var horarioUtc = LocalDateTime.of(2026, 11, 19, 17, 30);

        when(repository.findForUpdate(freteId)).thenReturn(Optional.of(
                new Freight(freteId, clienteId, prestadorId, "PRONTO_INICIO")
        ));
        when(repository.markCancelled(freteId)).thenReturn(horarioUtc);

        service.cancel(freteId, clienteId, justificativa);

        var ordem = inOrder(repository);
        ordem.verify(repository).findForUpdate(freteId);
        ordem.verify(repository).markCancelled(freteId);
        ordem.verify(repository).addCancellationEvent(freteId, clienteId, justificativa, horarioUtc);
        ordem.verify(repository).notifyCancellation(freteId, prestadorId, horarioUtc);
        ordem.verifyNoMoreInteractions();
    }
}
