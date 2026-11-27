package com.muv.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

class SubstituicaoServiceTest {
    private DesignacaoRepository repositorio;
    private ApplicationEventPublisher publisher;
    private DesignacaoService service;

    @BeforeEach
    void configurar() {
        repositorio = mock(DesignacaoRepository.class);
        publisher = mock(ApplicationEventPublisher.class);
        service = new DesignacaoService(repositorio, new CompatibilidadeDesignacaoService(),
                new NotificacaoInternaService(publisher));
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
        verify(publisher, never()).publishEvent(any(NotificacaoInternaSolicitada.class));
    }

    @Test
    void substituiSomenteMotoristaEPreservaVeiculoEmProntoInicio() {
        prepararFrete("PRONTO_INICIO");
        prepararRecursos(31L, 20L);
        when(repositorio.registrarSubstituicao(1L, 10L, 31L, 20L, 1L, "Troca de escala"))
                .thenReturn(2L);

        var resultado = service.substituir(1L, 10L, comando(31L, null));

        assertThat(resultado.status()).isEqualTo("PRONTO_INICIO");
        assertThat(resultado.revisao()).isEqualTo(2L);
        verify(repositorio).registrarSubstituicao(
                1L, 10L, 31L, 20L, 1L, "Troca de escala");
        verify(publisher).publishEvent(new NotificacaoInternaSolicitada(
                40L, "Recursos do frete atualizados",
                "O prestador substituiu o motorista ou o ve\u00edculo do seu frete.",
                1L, null, null));
    }

    @Test
    void substituiSomenteVeiculoEPreservaInterrupcao() {
        prepararFrete("INTERROMPIDO");
        prepararRecursos(30L, 21L);
        when(repositorio.registrarSubstituicao(1L, 10L, 30L, 21L, 1L, "Troca de escala"))
                .thenReturn(2L);

        var resultado = service.substituir(1L, 10L, comando(null, 21L));

        assertThat(resultado.status()).isEqualTo("INTERROMPIDO");
        verify(publisher).publishEvent(new NotificacaoInternaSolicitada(
                40L, "Recursos do frete atualizados",
                "O prestador substituiu o motorista ou o ve\u00edculo do seu frete.",
                1L, null, null));
    }

    @Test
    void solicitaAcordoNotificaClienteSomenteAposRegistrarAcordo() {
        prepararFrete("PRONTO_INICIO");
        prepararRecursos(31L, 21L);
        when(repositorio.buscarVeiculoParaValidacao(20L))
                .thenReturn(Optional.of(new VeiculoRecurso(20L, 10L, "Fiat", "Ducato",
                        2024, "RF9A020", "Branca", "ENCOMENDA", true, 0,
                        new BigDecimal("1200"))));
        when(repositorio.registrarSolicitacaoAcordo(1L, 10L, 31L, 21L, 1L,
                new BigDecimal("800.00"), "Nova condição", "Troca de escala"))
                .thenReturn(81L);

        var resultado = service.substituir(1L, 10L,
                new ComandoSubstituicao(31L, 21L, "Troca de escala", 1L,
                        new BigDecimal("800.00"), "Nova condição"));

        assertThat(resultado.solicitacaoAcordoId()).isEqualTo(81L);
        verify(publisher).publishEvent(new NotificacaoInternaSolicitada(
                40L, "Nova proposta de alteração do frete",
                "O prestador solicitou sua aprovação para alterar o frete.",
                1L, null, null));
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
