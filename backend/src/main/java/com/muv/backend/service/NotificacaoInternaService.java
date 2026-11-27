package com.muv.backend.service;

import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoInternaService {
    private final ApplicationEventPublisher eventos;

    public NotificacaoInternaService(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    public void notificar(long destinatarioId, String titulo, String mensagem,
                          Long freteId, Long solicitacaoId) {
        notificar(destinatarioId, titulo, mensagem, freteId, solicitacaoId, null);
    }

    public void notificar(long destinatarioId, String titulo, String mensagem,
                          Long freteId, Long solicitacaoId, LocalDateTime criadoEm) {
        eventos.publishEvent(new NotificacaoInternaSolicitada(
                destinatarioId, titulo, mensagem, freteId, solicitacaoId, criadoEm));
    }
}
