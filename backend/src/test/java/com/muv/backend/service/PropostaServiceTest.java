package com.muv.backend.service;

import com.muv.backend.dto.AvaliacaoSolicitacao;
import com.muv.backend.dto.RespostaPrestador;
import com.muv.backend.repository.PropostaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PropostaServiceTest {
    final PropostaRepository repo = mock(PropostaRepository.class);
    final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    final EstimativaFreteService estimativas = mock(EstimativaFreteService.class);
    final PercursoService percurso = mock(PercursoService.class);
    final NotificacaoInternaService notificacoes = new NotificacaoInternaService(publisher);
    final PropostaService service = new PropostaService(repo, notificacoes, estimativas, percurso, TransactionOperations.withoutTransaction());
    final BigDecimal original = new BigDecimal("315.78");
    final UUID chave = UUID.randomUUID();

    void preparar(String status, boolean bloquear, BigDecimal preco) {
        preparar(status, bloquear, preco, false);
    }

    void preparar(String status, boolean bloquear, BigDecimal preco, boolean propostaEnviada) {
        var detalhe = new AvaliacaoSolicitacao(9, "PESSOA", status, "Cliente", null, "A", "B", 3,
                null,null,null,null,null,preco,"Pedágios inclusos",null,List.of(),null,null,null,null,propostaEnviada,null);
        when(repo.consultar(9,4,false)).thenReturn(Optional.of(new PropostaRepository.Contexto(detalhe,18)));
        when(repo.consultar(9,4,bloquear)).thenReturn(Optional.of(new PropostaRepository.Contexto(detalhe,18)));
    }

    RespostaPrestador resposta(RespostaPrestador.Tipo tipo, BigDecimal valor, String motivo) {
        return new RespostaPrestador(tipo,original,"Pedágios inclusos",valor,motivo,chave);
    }

    @Test void aceitaValorExatamenteNoTeto() {
        prepararTeto(new BigDecimal("204.30"), "referencia-atual");
        var valor = new BigDecimal("204.30");
        service.responder(9, 4, ajustar(valor, "referencia-atual"));

        verify(repo).registrar(9, 4, valor, "Pedágios inclusos\nInclui espera", chave);
        verify(publisher).publishEvent(any(NotificacaoInternaSolicitada.class));
    }

    @Test void rejeitaCentavoAcimaDoTetoEAceiteDeOfertaAcimaDoTeto() {
        prepararTeto(new BigDecimal("204.30"), "referencia-atual");
        var manter = new RespostaPrestador(RespostaPrestador.Tipo.MANTER, original, "Pedágios inclusos",
                null, null, chave, "referencia-atual");
        for (var resposta : List.of(ajustar(new BigDecimal("204.31"), "referencia-atual"), manter)) {
            var erro = assertThrows(PropostaService.LimitePropostaException.class,
                    () -> service.responder(9, 4, resposta));
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, erro.getStatusCode());
            assertEquals("VALOR_ACIMA_DO_TETO", erro.codigo());
            assertEquals(new BigDecimal("204.30"), erro.teto());
        }
        verificarNenhumEnvio();
    }

    @Test void referenciaMudouOuNaoFoiConfirmada() {
        prepararTeto(new BigDecimal("204.30"), "referencia-atual");
        for (var versao : new String[] {"referencia-anterior", null}) {
            var erro = assertThrows(PropostaService.LimitePropostaException.class,
                    () -> service.responder(9, 4, ajustar(BigDecimal.TEN, versao)));
            assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
            assertEquals("ESTIMATIVA_DESATUALIZADA", erro.codigo());
        }
        verificarNenhumEnvio();
    }

    @Test void bloqueiaPropostaSemEstimativaEPermiteRecusa() {
        prepararTeto(null, null);
        var erro = assertThrows(PropostaService.LimitePropostaException.class,
                () -> service.responder(9, 4, ajustar(BigDecimal.TEN, null)));
        assertEquals("ESTIMATIVA_INDISPONIVEL", erro.codigo());

        service.recusar(9, 4, "Sem veículo disponível");
        verify(repo).recusar(9, 4, "Sem veículo disponível");
        verificarNenhumEnvio();
    }

    @Test void reenvioIdempotentePreservaPropostaJaRegistradaMesmoAcimaDoTeto() {
        prepararTeto(new BigDecimal("204.30"), "referencia-atual");
        when(repo.envioAnterior(9, 4, chave))
                .thenReturn(Optional.of(new PropostaRepository.Registro(25, original, "Pedágios inclusos")));
        assertEquals(25, service.responder(9, 4, resposta(RespostaPrestador.Tipo.MANTER, null, null)));
        verificarNenhumEnvio();
    }

    private void prepararTeto(BigDecimal teto, String versao) {
        preparar("ABERTA", true, original);
        when(estimativas.tetoAtivo("PESSOA")).thenReturn(true);
        when(percurso.consultar(any())).thenReturn(new PercursoService.Percurso(
                List.of(), List.of(), List.of(), null));
        when(estimativas.limiteProposta(any(), isNull())).thenReturn(
                new EstimativaFreteService.LimiteProposta(true, teto, versao,
                        teto == null ? "Distância viária indisponível." : null));
    }

    private RespostaPrestador ajustar(BigDecimal valor, String versao) {
        return new RespostaPrestador(RespostaPrestador.Tipo.AJUSTAR, original, "Pedágios inclusos",
                valor, "Inclui espera", chave, versao);
    }

    private void verificarNenhumEnvio() {
        verify(repo, never()).registrar(anyLong(), anyLong(), any(), any(), any());
        verifyNoInteractions(publisher);
    }

    @Test void manterValor() {
        preparar("ABERTA",true,original);
        service.responder(9,4,resposta(RespostaPrestador.Tipo.MANTER,null,null));
        verify(repo).registrar(9,4,original,"Pedágios inclusos",chave);
        verify(publisher).publishEvent(new NotificacaoInternaSolicitada(
                18, "Nova proposta", "Você recebeu uma proposta para sua solicitação.", null, 9L, null));
    }

    @Test void bloqueiaSegundaPropostaSemNotificar() {
        preparar("ABERTA", true, original, true);

        assertThrows(ResponseStatusException.class,
                () -> service.responder(9, 4, resposta(RespostaPrestador.Tipo.MANTER, null, null)));

        verify(repo, never()).registrar(anyLong(), anyLong(), any(), any(), any());
        verifyNoInteractions(publisher);
    }

    @Test void repeticaoIdempotenteNaoDuplicaNotificacao() {
        preparar("ABERTA", true, original);
        var resposta = resposta(RespostaPrestador.Tipo.MANTER, null, null);
        var registrada = new PropostaRepository.Registro(72L, original, "Pedágios inclusos");
        when(repo.envioAnterior(9, 4, chave)).thenReturn(Optional.empty(), Optional.of(registrada));
        when(repo.registrar(9, 4, original, "Pedágios inclusos", chave)).thenReturn(72L);

        assertEquals(72L, service.responder(9, 4, resposta));
        assertEquals(72L, service.responder(9, 4, resposta));

        verify(repo, times(1)).registrar(9, 4, original, "Pedágios inclusos", chave);
        verify(publisher, times(1)).publishEvent(any(NotificacaoInternaSolicitada.class));
    }

    @Test void aceitaMensagemNoLimite() {
        preparar("ABERTA",true,original);
        var msg = "a".repeat(500);
        service.responder(9,4,resposta(RespostaPrestador.Tipo.AJUSTAR, BigDecimal.TEN,msg));
        verify(repo).registrar(9,4,BigDecimal.TEN,"Pedágios inclusos\n"+msg,chave);
    }

    @Test void validaPrecoEMensagem() {
        preparar("ABERTA",true,original);
        for (String msg : List.of("", "  ", "a".repeat(501)))
            assertThrows(ResponseStatusException.class,()->service.responder(9,4,resposta(RespostaPrestador.Tipo.AJUSTAR,BigDecimal.ONE,msg)));
        for (String valor : List.of("0","-2","1.234","100000000000"))
            assertThrows(ResponseStatusException.class,()->service.responder(9,4,resposta(RespostaPrestador.Tipo.AJUSTAR,new BigDecimal(valor),"Motivo")));
        verifyNoInteractions(publisher);
    }

    @ParameterizedTest @ValueSource(strings={"CANCELADA","CONTRATADA"})
    void encerrouAntesDoEnvio(String status) {
        preparar(status,true,original);
        assertThrows(ResponseStatusException.class,()->service.responder(9,4,resposta(RespostaPrestador.Tipo.MANTER,null,null)));
        assertThrows(ResponseStatusException.class,()->service.recusar(9,4,"Motivo"));
        verifyNoInteractions(publisher);
    }

    @Test void leRegistroLegadoSemPreco() {
        preparar("ABERTA",false,null);
        assertNull(service.avaliar(9,4).valorCliente());
    }

    @Test void ofertaMudou() {
        preparar("ABERTA",true,BigDecimal.ONE);
        assertThrows(ResponseStatusException.class,()->service.responder(9,4,resposta(RespostaPrestador.Tipo.MANTER,null,null)));
        verify(repo,never()).registrar(anyLong(),anyLong(),any(),any(),any());
    }

    @Test void negaSolicitacaoInacessivel() {
        assertThrows(ResponseStatusException.class,()->service.avaliar(9,4));
        assertThrows(ResponseStatusException.class,()->service.responder(9,4,resposta(RespostaPrestador.Tipo.MANTER,null,null)));
    }
}
