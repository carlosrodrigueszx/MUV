package com.muv.backend.service;

import com.muv.backend.dto.DesignacaoFrete.VeiculoRecurso;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CompatibilidadeDesignacaoService {

    public Resultado avaliar(
            long prestadorId,
            String modalidadeSolicitacao,
            Integer quantidadePessoas,
            BigDecimal pesoEstimadoKg,
            VeiculoRecurso veiculo) {
        List<String> motivos = new ArrayList<>();

        if (veiculo.prestadorId() != prestadorId) {
            motivos.add("VEICULO_DE_OUTRO_PRESTADOR");
        }
        if (!veiculo.ativo()) {
            motivos.add("VEICULO_INATIVO");
        }
        if (!modalidadeCompativel(modalidadeSolicitacao, veiculo.modalidade())) {
            motivos.add("MODALIDADE_INCOMPATIVEL");
        } else if ("PESSOA".equals(modalidadeSolicitacao)
                && quantidadePessoas != null
                && veiculo.capacidadePassageiros() < quantidadePessoas) {
            motivos.add("CAPACIDADE_PASSAGEIROS_INSUFICIENTE");
        } else if ("ENCOMENDA".equals(modalidadeSolicitacao)
                && pesoEstimadoKg != null
                && veiculo.capacidadeCargaKg().compareTo(pesoEstimadoKg) < 0) {
            motivos.add("CAPACIDADE_CARGA_INSUFICIENTE");
        }

        return new Resultado(motivos.isEmpty(), List.copyOf(motivos));
    }

    private boolean modalidadeCompativel(String solicitacao, String veiculo) {
        return "AMBOS".equals(veiculo) || solicitacao.equals(veiculo);
    }

    public record Resultado(boolean compativel, List<String> motivos) {}
}
