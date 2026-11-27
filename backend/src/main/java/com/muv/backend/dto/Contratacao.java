package com.muv.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Contratacao {
    private Contratacao() {}

    public record Solicitacao(long id, String modalidade, String status, String origem,
                              String destino, LocalDateTime dataPretendida) {}

    public record Proposta(long id, long prestadorId, String prestador, String tipoPrestador,
                           BigDecimal avaliacao, boolean autorizado, BigDecimal valor,
                           String condicoes, String status) {}

    public record Comparacao(Solicitacao solicitacao, List<Proposta> propostas, Long freteId) {}

    public record Conclusao(LocalDateTime realizadaEm, String nomeRecebedor) {}

    public record Frete(long id, long propostaId, String prestador, BigDecimal valor,
                       String condicoes, String status, LocalDateTime contratadoEm,
                       @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                       Conclusao conclusao) {}

    public record Detalhe(Solicitacao solicitacao, Frete frete, List<Proposta> outrasPropostas) {}

    public record Aceite(long id, boolean criado) {}
}
