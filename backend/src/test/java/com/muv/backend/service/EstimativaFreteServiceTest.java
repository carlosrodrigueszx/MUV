package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.muv.backend.dto.AvaliacaoSolicitacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class EstimativaFreteServiceTest {
    private static final String PREFIXO = "muv.estimativa.pessoa.";
    private final PercursoService percurso = mock(PercursoService.class);
    private final MockEnvironment configuracao = new MockEnvironment()
            .withProperty(PREFIXO + "consumo-km-litro", "10")
            .withProperty(PREFIXO + "preco-litro", "6.10")
            .withProperty(PREFIXO + "fonte-combustivel", "Referência controlada de teste")
            .withProperty(PREFIXO + "data-combustivel", LocalDate.now(ZoneId.of("America/Fortaleza")).toString())
            .withProperty(PREFIXO + "validade-dias", "1")
            .withProperty(PREFIXO + "operacao-por-km", "0.40")
            .withProperty(PREFIXO + "remuneracao", "15")
            .withProperty(PREFIXO + "alerta-percentual", "25");
    private final EstimativaFreteService service = new EstimativaFreteService(configuracao, percurso);

    @Test
    void calculaComponentesComPisoDesativadoPorPadrao() {
        var enderecos = List.of("Origem", "Parada", "Destino");
        when(percurso.distanciaKm(enderecos)).thenReturn(new BigDecimal("120"));

        var estimativa = service.calcularParaEnderecos("PESSOA", enderecos);

        assertThat(estimativa.combustivel()).isEqualByComparingTo("73.20");
        assertThat(estimativa.custosOperacionais()).isEqualByComparingTo("48.00");
        assertThat(estimativa.valorEstimado()).isEqualByComparingTo("136.20");
        assertThat(estimativa.limiarAlerta()).isEqualByComparingTo("170.25");
        assertThat(estimativa.piso()).isNull();
    }

    @Test
    void arredondaPisoParaCimaEExigeCustosOperacionais() {
        configuracao.setProperty(PREFIXO + "piso-fator", "0.333");
        configuracao.setProperty(PREFIXO + "piso-minimo", "0");
        assertThat(service.calcular("PESSOA", new BigDecimal("120")).piso()).isEqualByComparingTo("40.36");

        configuracao.setProperty(PREFIXO + "operacao-por-km", "0");
        assertThat(service.calcular("PESSOA", new BigDecimal("120")).piso()).isNull();
    }

    @Test
    void calculaTetoComArredondamentoParaBaixoEVersaoSensivelAoPerfil() {
        var solicitacao = mock(AvaliacaoSolicitacao.class);
        when(solicitacao.id()).thenReturn(907031L);
        when(solicitacao.modalidade()).thenReturn("PESSOA");
        var distancia = new BigDecimal("120.01");

        assertThat(service.limiteProposta(solicitacao, service.calcular("PESSOA", distancia)).ativo()).isFalse();
        configuracao.setProperty(PREFIXO + "teto-ativo", "true");
        var limite = service.limiteProposta(solicitacao, service.calcular("PESSOA", distancia));
        assertThat(limite.teto()).isEqualByComparingTo("204.31");
        assertThat(service.limiteProposta(solicitacao, service.calcular("PESSOA", distancia)).versao())
                .isEqualTo(limite.versao());

        configuracao.setProperty(PREFIXO + "preco-litro", "6.20");
        assertThat(service.limiteProposta(solicitacao, service.calcular("PESSOA", distancia)).versao())
                .isNotEqualTo(limite.versao());
    }

    @Test
    void aplicaTetoDeEncomendaSemAtivarTetoDePessoas() {
        String encomenda = "muv.estimativa.encomenda.";
        configuracao.setProperty(encomenda + "consumo-km-litro", "8");
        configuracao.setProperty(encomenda + "preco-litro", "6");
        configuracao.setProperty(encomenda + "fonte-combustivel", "Referência controlada de teste");
        configuracao.setProperty(encomenda + "data-combustivel", configuracao.getRequiredProperty(PREFIXO + "data-combustivel"));
        configuracao.setProperty(encomenda + "validade-dias", "1");
        configuracao.setProperty(encomenda + "operacao-por-km", "0.50");
        configuracao.setProperty(encomenda + "remuneracao", "20");
        configuracao.setProperty(encomenda + "alerta-percentual", "25");
        configuracao.setProperty(encomenda + "teto-ativo", "true");
        var solicitacao = mock(AvaliacaoSolicitacao.class);
        when(solicitacao.modalidade()).thenReturn("ENCOMENDA");

        var estimativa = service.calcular("ENCOMENDA", new BigDecimal("80"));
        assertThat(estimativa.valorEstimado()).isEqualByComparingTo("120.00");
        assertThat(service.limiteProposta(solicitacao, estimativa).teto()).isEqualByComparingTo("180.00");
        assertThat(service.tetoAtivo("PESSOA")).isFalse();
    }

    @Test
    void sinalizaPercursoAusenteEIgnoraPerfilVencido() {
        assertThat(service.calcular("PESSOA", null).motivo()).contains("Distância");
        configuracao.setProperty(PREFIXO + "data-combustivel", "2000-01-01");
        assertThat(service.calcularParaEnderecos("PESSOA", List.of("Origem", "Destino")).valorEstimado()).isNull();
        assertThat(service.calcular("ENCOMENDA", BigDecimal.TEN).valorEstimado()).isNull();
        verifyNoInteractions(percurso);
    }
}
