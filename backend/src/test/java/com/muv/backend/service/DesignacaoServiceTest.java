package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.muv.backend.dto.DesignacaoFrete.ComandoDesignacao;
import com.muv.backend.dto.DesignacaoFrete.VeiculoRecurso;
import com.muv.backend.repository.DesignacaoRepository;
import com.muv.backend.repository.DesignacaoRepository.FreteBloqueado;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DesignacaoServiceTest {
    private DesignacaoRepository repositorio;
    private DesignacaoService service;

    @BeforeEach
    void configurar() {
        repositorio = mock(DesignacaoRepository.class);
        service = new DesignacaoService(repositorio, new CompatibilidadeDesignacaoService());
    }

    @Test
    void exigeMotoristaEVeiculo() {
        prepararFrete();
        assertStatus(400, () -> service.designar(1L, 10L, new ComandoDesignacao(null, 20L, 0)));
        assertStatus(400, () -> service.designar(1L, 10L, new ComandoDesignacao(30L, null, 0)));
        verify(repositorio, times(2)).bloquearFrete(1L, 10L);
    }

    @Test
    void rejeitaMotoristaSemVinculoVerificado() {
        prepararFrete();
        when(repositorio.motoristaVerificado(10L, 30L)).thenReturn(false);

        assertStatus(422, () -> service.designar(1L, 10L, comando()));
        verify(repositorio, never()).buscarVeiculoParaValidacao(20L);
    }

    @Test
    void rejeitaVeiculoInexistente() {
        prepararFrete();
        when(repositorio.motoristaVerificado(10L, 30L)).thenReturn(true);
        when(repositorio.buscarVeiculoParaValidacao(20L)).thenReturn(Optional.empty());

        assertStatus(422, () -> service.designar(1L, 10L, comando()));
    }

    @Test
    void rejeitaVeiculoAlheioInativoDeModalidadeErradaOuSemCapacidade() {
        assertVeiculoInvalido(veiculo(11L, "ENCOMENDA", true, 5), "VEICULO_DE_OUTRO_PRESTADOR");
        assertVeiculoInvalido(veiculo(10L, "ENCOMENDA", false, 5), "VEICULO_INATIVO");
        assertVeiculoInvalido(veiculo(10L, "PESSOA", true, 5), "MODALIDADE_INCOMPATIVEL");
        assertVeiculoInvalido(veiculo(10L, "ENCOMENDA", true, 1), "CAPACIDADE_CARGA_INSUFICIENTE");
    }

    @Test
    void aceitaSomenteRecursosRevalidadosAposBloqueio() {
        prepararFrete();
        when(repositorio.motoristaVerificado(10L, 30L)).thenReturn(true);
        when(repositorio.buscarVeiculoParaValidacao(20L))
                .thenReturn(Optional.of(veiculo(10L, "ENCOMENDA", true, 200)));
        when(repositorio.registrarDesignacao(1L, 10L, 30L, 20L, 0L)).thenReturn(1L);

        var resultado = service.designar(1L, 10L, comando());

        assertThat(resultado.status()).isEqualTo("PRONTO_INICIO");
        assertThat(resultado.revisao()).isEqualTo(1L);
        verify(repositorio).registrarDesignacao(1L, 10L, 30L, 20L, 0L);
    }

    private void assertVeiculoInvalido(VeiculoRecurso veiculo, String motivo) {
        configurar();
        prepararFrete();
        when(repositorio.motoristaVerificado(10L, 30L)).thenReturn(true);
        when(repositorio.buscarVeiculoParaValidacao(20L)).thenReturn(Optional.of(veiculo));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.designar(1L, 10L, comando()));

        assertThat(erro.getStatusCode().value()).isEqualTo(422);
        assertThat(erro.getReason()).contains(motivo);
        verify(repositorio, never()).registrarDesignacao(1L, 10L, 30L, 20L, 0L);
    }

    private void prepararFrete() {
        when(repositorio.bloquearFrete(1L, 10L)).thenReturn(Optional.of(
                new FreteBloqueado(1L, "AGUARDANDO_DESIGNACAO", 0L,
                        "ENCOMENDA", null, new BigDecimal("180"), false,
                        null, null, 40L, new BigDecimal("480"), "Condições")));
    }

    private ComandoDesignacao comando() {
        return new ComandoDesignacao(30L, 20L, 0L);
    }

    private VeiculoRecurso veiculo(
            long prestador,
            String modalidade,
            boolean ativo,
            int carga) {
        return new VeiculoRecurso(20L, prestador, "Fiat", "Ducato", 2024,
                "RF9A020", "Branca", modalidade, ativo, 0, BigDecimal.valueOf(carga));
    }

    private void assertStatus(int status, org.junit.jupiter.api.function.Executable executavel) {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class, executavel);
        assertThat(erro.getStatusCode().value()).isEqualTo(status);
    }
}
