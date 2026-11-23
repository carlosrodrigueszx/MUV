package com.muv.backend.service;

import com.muv.backend.dto.Contratacao.*;
import com.muv.backend.repository.ContratacaoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContratacaoService {
    private final ContratacaoRepository repositorio;
    private final ApplicationEventPublisher eventos;

    public ContratacaoService(ContratacaoRepository repositorio, ApplicationEventPublisher eventos) {
        this.repositorio = repositorio;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Comparacao comparar(long id, long cliente) {
        var solicitacao = localizar(id, cliente, false);
        var frete = repositorio.freteDaSolicitacao(id).map(Frete::id).orElse(null);
        return new Comparacao(solicitacao, repositorio.propostas(id), frete);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Detalhe detalhe(long id, long cliente) {
        long solicitacaoId = repositorio.solicitacaoDoFrete(id, cliente)
                .orElseThrow(ContratacaoService::indisponivel);
        var solicitacao = localizar(solicitacaoId, cliente, false);
        var frete = repositorio.freteDaSolicitacao(solicitacaoId).orElseThrow(ContratacaoService::indisponivel);
        var outras = repositorio.propostas(solicitacaoId).stream()
                .filter(proposta -> proposta.id() != frete.propostaId()).toList();
        return new Detalhe(solicitacao, frete, outras);
    }

    @Transactional
    public Aceite aceitar(long id, long propostaId, long cliente) {
        var solicitacao = localizar(id, cliente, true);
        var existente = repositorio.freteDaSolicitacao(id);
        if (existente.isPresent()) {
            var frete = existente.get();
            if (frete.propostaId() == propostaId) return new Aceite(frete.id(), false);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A solicitação já foi contratada.");
        }
        if (!"ABERTA".equals(solicitacao.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A solicitação está encerrada.");
        }
        var proposta = repositorio.propostas(id).stream()
                .filter(item -> item.id() == propostaId)
                .findFirst().orElseThrow(ContratacaoService::indisponivel);
        if (!"ENVIADA".equals(proposta.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A proposta não está disponível para aceite.");
        }
        long frete = repositorio.registrar(id, proposta);
        eventos.publishEvent(new ContratacaoConfirmada(proposta.prestadorId(), id, frete));
        return new Aceite(frete, true);
    }

    private Solicitacao localizar(long id, long cliente, boolean bloquear) {
        return repositorio.solicitacao(id, cliente, bloquear).orElseThrow(ContratacaoService::indisponivel);
    }

    private static ResponseStatusException indisponivel() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    record ContratacaoConfirmada(long prestador, long solicitacao, long frete) {}
}

@org.springframework.stereotype.Component
class NotificacaoContratacao {
    private final NotificacaoInternaService notificacoes;

    NotificacaoContratacao(NotificacaoInternaService notificacoes) {
        this.notificacoes = notificacoes;
    }

    @EventListener
    void registrar(ContratacaoService.ContratacaoConfirmada evento) {
        notificacoes.notificar(evento.prestador(), "Contratação confirmada",
                "O cliente aceitou sua proposta de frete.", evento.frete(), evento.solicitacao());
    }
}
