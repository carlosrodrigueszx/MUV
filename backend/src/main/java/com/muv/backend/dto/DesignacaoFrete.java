package com.muv.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class DesignacaoFrete {
    private DesignacaoFrete() {}

    public record Contexto(
            long freteId,
            long solicitacaoId,
            String modalidade,
            String status,
            LocalDateTime dataPretendida,
            String origem,
            String destino,
            BigDecimal valor,
            String condicoes,
            long revisao,
            Integer quantidadePessoas,
            BigDecimal pesoEstimadoKg,
            boolean prestadorAutonomo,
            DesignacaoAtual designacaoAtual,
            List<MotoristaRecurso> motoristas,
            List<VeiculoRecurso> veiculos) {}

    public record Consulta(
            long freteId,
            long solicitacaoId,
            String modalidade,
            String status,
            LocalDateTime dataPretendida,
            String origem,
            String destino,
            BigDecimal valor,
            String condicoes,
            long revisao,
            Integer quantidadePessoas,
            BigDecimal pesoEstimadoKg,
            boolean prestadorAutonomo,
            DesignacaoAtual designacaoAtual,
            List<MotoristaRecurso> motoristas,
            List<VeiculoDisponivel> veiculos) {}

    public record DesignacaoAtual(
            Long motoristaId,
            String motoristaNome,
            Long veiculoId,
            String veiculoMarca,
            String veiculoModelo,
            String veiculoPlaca) {}

    public record MotoristaRecurso(long id, String nome) {}

    public record ComandoDesignacao(Long motoristaId, Long veiculoId, long revisao) {}

    public record DesignacaoConfirmada(
            long freteId,
            String status,
            long revisao,
            DesignacaoAtual designacaoAtual) {}

    public record ComandoSubstituicao(
            Long motoristaId,
            Long veiculoId,
            String justificativa,
            long revisao,
            BigDecimal novoValor,
            String novasCondicoes) {}

    public record SubstituicaoConfirmada(
            long freteId,
            String status,
            long revisao,
            DesignacaoAtual designacaoAtual,
            Long solicitacaoAcordoId) {}

    public record RespostaAcordo(boolean aceitar) {}

    public record AcordoRespondido(
            long acordoId,
            String status,
            boolean aplicado,
            long revisaoFrete) {}

    public record ConsultaCliente(
            long freteId,
            String status,
            String motoristaNome,
            String veiculoMarca,
            String veiculoModelo,
            String veiculoPlaca) {}

    public record VeiculoRecurso(
            long id,
            long prestadorId,
            String marca,
            String modelo,
            int ano,
            String placa,
            String cor,
            String modalidade,
            boolean ativo,
            int capacidadePassageiros,
            BigDecimal capacidadeCargaKg) {}

    public record VeiculoDisponivel(
            long id,
            String marca,
            String modelo,
            int ano,
            String placa,
            String cor,
            String modalidade,
            boolean ativo,
            int capacidadePassageiros,
            BigDecimal capacidadeCargaKg,
            boolean compativel,
            List<String> incompatibilidades) {}
}
