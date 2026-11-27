package com.muv.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CentralNotificacoes(List<Item> itens, long naoLidas) {
    public record Item(long id, String titulo, String mensagem, LocalDateTime criadoEm, boolean lida,
                       Long freteId, Long solicitacaoId, String acaoRotulo, String acaoUrl) {}
}
