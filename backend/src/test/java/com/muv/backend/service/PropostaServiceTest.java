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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.ApplicationEventPublisher;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PropostaServiceTest {
    final PropostaRepository repo = mock(PropostaRepository.class);
    final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    final PropostaService service = new PropostaService(repo, new NotificacaoInternaService(publisher));
    final BigDecimal original = new BigDecimal("315.78");
    final UUID chave = UUID.randomUUID();

    void preparar(String status, boolean bloquear, BigDecimal preco) {
        preparar(status, bloquear, preco, false);
    }

    void preparar(String status, boolean bloquear, BigDecimal preco, boolean propostaEnviada) {
        var detalhe = new AvaliacaoSolicitacao(9, "PESSOA", status, "Cliente", null, "A", "B", 3,
                null,null,null,null,null,preco,"Pedágios inclusos",null,List.of(),null,null,null,null,propostaEnviada);
        when(repo.consultar(9,4,bloquear)).thenReturn(Optional.of(new PropostaRepository.Contexto(detalhe,18)));
    }

    RespostaPrestador resposta(RespostaPrestador.Tipo tipo, BigDecimal valor, String motivo) {
        return new RespostaPrestador(tipo,original,"Pedágios inclusos",valor,motivo,chave);
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
