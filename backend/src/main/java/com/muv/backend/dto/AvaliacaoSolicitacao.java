package com.muv.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A criação da solicitação grava a oferta original em solicitacao_frete.valor_cliente (BRL)
 * e solicitacao_frete.condicoes_cliente. A avaliação expõe ambos os campos, que podem
 * ser nulos em registros legados. Cada oferta do prestador é registrada na tabela proposta.
 */
public record AvaliacaoSolicitacao(long id, String modalidade, String status, String cliente,
        LocalDateTime partida, String origem, String destino, Integer passageiros, String bagagens,
        String volumes, BigDecimal pesoKg, String coleta, String entrega,
        BigDecimal valorCliente, String condicoesCliente, String justificativaRecusa, java.util.List<Parada> paradas,
        String mapaCaminho, BigDecimal percursoKm, Integer deslocamentoMinutos, Integer duracaoMinutos, boolean propostaEnviada, LocalDateTime retornoPrevisto) {
    public record Parada(String endereco, LocalDateTime horario) {}
}
