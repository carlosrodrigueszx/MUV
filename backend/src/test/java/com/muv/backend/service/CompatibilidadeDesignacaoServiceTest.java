package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.muv.backend.dto.DesignacaoFrete.VeiculoRecurso;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CompatibilidadeDesignacaoServiceTest {
    private final CompatibilidadeDesignacaoService service = new CompatibilidadeDesignacaoService();

    @Test
    void aceitaVeiculoDePessoasNoLimiteExato() {
        var resultado = service.avaliar(10L, "PESSOA", 4, null,
                veiculo(10L, "PESSOA", true, 4, "0"));

        assertThat(resultado.compativel()).isTrue();
        assertThat(resultado.motivos()).isEmpty();
    }

    @Test
    void aceitaVeiculoDeEncomendaNoLimiteExato() {
        var resultado = service.avaliar(10L, "ENCOMENDA", null, new BigDecimal("100.50"),
                veiculo(10L, "ENCOMENDA", true, 0, "100.50"));

        assertThat(resultado.compativel()).isTrue();
    }

    @Test
    void aceitaModalidadeAmbosParaPessoasEEncomendas() {
        assertThat(service.avaliar(10L, "PESSOA", 2, null,
                veiculo(10L, "AMBOS", true, 2, "200")).compativel()).isTrue();
        assertThat(service.avaliar(10L, "ENCOMENDA", null, new BigDecimal("200"),
                veiculo(10L, "AMBOS", true, 2, "200")).compativel()).isTrue();
    }

    @Test
    void rejeitaCapacidadeDePassageirosInsuficiente() {
        var resultado = service.avaliar(10L, "PESSOA", 5, null,
                veiculo(10L, "PESSOA", true, 4, "0"));

        assertThat(resultado.motivos()).containsExactly("CAPACIDADE_PASSAGEIROS_INSUFICIENTE");
    }

    @Test
    void rejeitaCapacidadeDeCargaInsuficiente() {
        var resultado = service.avaliar(10L, "ENCOMENDA", null, new BigDecimal("100.51"),
                veiculo(10L, "ENCOMENDA", true, 0, "100.50"));

        assertThat(resultado.motivos()).containsExactly("CAPACIDADE_CARGA_INSUFICIENTE");
    }

    @Test
    void rejeitaVeiculoInativoAlheioEOuDeModalidadeErrada() {
        var resultado = service.avaliar(10L, "PESSOA", 2, null,
                veiculo(20L, "ENCOMENDA", false, 0, "500"));

        assertThat(resultado.compativel()).isFalse();
        assertThat(resultado.motivos()).containsExactly(
                "VEICULO_DE_OUTRO_PRESTADOR",
                "VEICULO_INATIVO",
                "MODALIDADE_INCOMPATIVEL");
    }

    @Test
    void naoInfereVolumeCategoriaOuAgenda() {
        var resultado = service.avaliar(10L, "ENCOMENDA", null, new BigDecimal("50"),
                veiculo(10L, "ENCOMENDA", true, 0, "50"));

        assertThat(resultado.compativel()).isTrue();
        assertThat(resultado.motivos()).isEmpty();
    }

    private VeiculoRecurso veiculo(
            long prestadorId,
            String modalidade,
            boolean ativo,
            int capacidadePessoas,
            String capacidadeCarga) {
        return new VeiculoRecurso(
                1L,
                prestadorId,
                "Marca",
                "Modelo",
                2026,
                "RF9A001",
                "Branca",
                modalidade,
                ativo,
                capacidadePessoas,
                new BigDecimal(capacidadeCarga));
    }
}
