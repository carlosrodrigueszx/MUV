package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.muv.backend.dto.DesignacaoFrete.ComandoSubstituicao;
import com.muv.backend.dto.DesignacaoFrete.VeiculoRecurso;
import com.muv.backend.repository.DesignacaoRepository;
import com.muv.backend.repository.DesignacaoRepository.FreteBloqueado;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SubstituicaoServiceTest {
    private DesignacaoRepository repositorio;
    private DesignacaoService service;

    @BeforeEach
    void configurar() {
        repositorio = mock(DesignacaoRepository.class);
        service = new DesignacaoService(repositorio, new CompatibilidadeDesignacaoService());
    }

    @Test
    void exigeJustificativa() {
        assertStatus(400, () -> service.substituir(1L, 10L,
                new ComandoSubstituicao(31L, null, "  ", 1L, null, null)));
    }

    @Test
    void rejeitaSubstituicaoDuranteExecucaoAtiva() {
        prepararFrete("EM_EXECUCAO");
        assertStatus(409, () -> service.substituir(1L, 10L, comando(31L, null)));
    }

    @Test
    void substituiSomenteMotoristaEPreservaVeiculoEmProntoInicio() {
        prepararFrete("PRONTO_INICIO");
        prepararRecursos(31L, 20L);
        when(repositorio.registrarSubstituicao(1L, 10L, 40L, 31L, 20L, 1L, "Troca de escala"))
                .thenReturn(2L);

        var resultado = service.substituir(1L, 10L, comando(31L, null));

        assertThat(resultado.status()).isEqualTo("PRONTO_INICIO");
        assertThat(resultado.revisao()).isEqualTo(2L);
        verify(repositorio).registrarSubstituicao(
                1L, 10L, 40L, 31L, 20L, 1L, "Troca de escala");
    }

    @Test
    void substituiSomenteVeiculoEPreservaInterrupcao() {
        prepararFrete("INTERROMPIDO");
        prepararRecursos(30L, 21L);
        when(repositorio.registrarSubstituicao(1L, 10L, 40L, 30L, 21L, 1L, "Troca de escala"))
                .thenReturn(2L);

        var resultado = service.substituir(1L, 10L, comando(null, 21L));

        assertThat(resultado.status()).isEqualTo("INTERROMPIDO");
    }

    private void prepararFrete(String status) {
        when(repositorio.bloquearFrete(1L, 10L)).thenReturn(Optional.of(new FreteBloqueado(
                1L, status, 1L, "ENCOMENDA", null, new BigDecimal("180"), false,
                30L, 20L, 40L, new BigDecimal("480"), "Condições")));
    }

    private void prepararRecursos(long motorista, long veiculo) {
        when(repositorio.motoristaVerificado(10L, motorista)).thenReturn(true);
        when(repositorio.buscarVeiculoParaValidacao(veiculo))
                .thenReturn(Optional.of(new VeiculoRecurso(veiculo, 10L, "Fiat", "Ducato",
                        2024, "RF9A020", "Branca", "ENCOMENDA", true, 0,
                        new BigDecimal("1200"))));
    }

    private ComandoSubstituicao comando(Long motorista, Long veiculo) {
        return new ComandoSubstituicao(motorista, veiculo, "Troca de escala", 1L, null, null);
    }

    private void assertStatus(int status, org.junit.jupiter.api.function.Executable executavel) {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class, executavel);
        assertThat(erro.getStatusCode().value()).isEqualTo(status);
    }
}
