package com.muv.backend.service;

import com.muv.backend.repository.NotificacaoRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ObservadorNotificacaoInterna {
    private final NotificacaoRepository notificacoes;

    public ObservadorNotificacaoInterna(NotificacaoRepository notificacoes) {
        this.notificacoes = notificacoes;
    }

    @EventListener
    public void registrar(NotificacaoInternaSolicitada evento) {
        if (evento.criadoEm() == null) {
            notificacoes.registrar(evento.destinatarioId(), evento.titulo(), evento.mensagem(),
                    evento.freteId(), evento.solicitacaoId());
        } else {
            notificacoes.registrar(evento.destinatarioId(), evento.titulo(), evento.mensagem(),
                    evento.freteId(), evento.solicitacaoId(), evento.criadoEm());
        }
    }
}
