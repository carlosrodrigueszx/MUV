package com.muv.backend.service;

import com.muv.backend.repository.NotificacaoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificacaoInternaServiceTest {
    @Test
    void publicaEventoComDestinatarioEContextoResolvidos() {
        var publisher = mock(ApplicationEventPublisher.class);
        var service = new NotificacaoInternaService(publisher);

        service.notificar(7, "Nova proposta", "Você recebeu uma proposta.", null, 23L);

        verify(publisher).publishEvent(new NotificacaoInternaSolicitada(
                7, "Nova proposta", "Você recebeu uma proposta.", null, 23L, null));
    }

    @Test
    void exigeVinculoComFreteOuSolicitacao() {
        assertThrows(IllegalArgumentException.class,
                () -> new NotificacaoInternaSolicitada(7, "Título", "Mensagem", null, null, null));
    }

    @Test
    void observerPersisteEventoPublicado() {
        var repository = mock(NotificacaoRepository.class);
        var observer = new ObservadorNotificacaoInterna(repository);
        var event = new NotificacaoInternaSolicitada(7, "Frete cancelado", "Consulte o motivo.", 3L, 23L, null);

        observer.registrar(event);

        verify(repository).registrar(7, "Frete cancelado", "Consulte o motivo.", 3L, 23L);
    }
}
