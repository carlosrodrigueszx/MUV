package com.muv.backend.service;

import java.time.LocalDateTime;

/** Minimal, recipient-resolved request for a persisted internal notification. */
public record NotificacaoInternaSolicitada(
        long destinatarioId,
        String titulo,
        String mensagem,
        Long freteId,
        Long solicitacaoId,
        LocalDateTime criadoEm) {
    public NotificacaoInternaSolicitada {
        if (destinatarioId <= 0) throw new IllegalArgumentException("Destinatário inválido.");
        if (titulo == null || titulo.isBlank() || titulo.length() > 64) {
            throw new IllegalArgumentException("Título da notificação inválido.");
        }
        if (mensagem == null || mensagem.isBlank() || mensagem.length() > 255) {
            throw new IllegalArgumentException("Mensagem da notificação inválida.");
        }
        if ((freteId == null || freteId <= 0) && (solicitacaoId == null || solicitacaoId <= 0)) {
            throw new IllegalArgumentException("A notificação precisa estar vinculada a um frete ou solicitação.");
        }
    }
}
