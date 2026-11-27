package com.muv.backend.service;

import com.muv.backend.repository.FreightCancellationRepository;
import com.muv.backend.repository.FreightCancellationRepository.CancellationEvent;
import com.muv.backend.repository.FreightCancellationRepository.Details;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FreightCancellationService {
    private final FreightCancellationRepository repository;
    private final NotificacaoInternaService notificacoes;

    public FreightCancellationService(FreightCancellationRepository repository,
                                     NotificacaoInternaService notificacoes) {
        this.repository = repository;
        this.notificacoes = notificacoes;
    }

    public record Confirmation(Details frete, String autorNome, String autorPapel, CancellationEvent registro) {}

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Confirmation details(long freightId, long authorId) {
        var freight = repository.findDetails(freightId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Frete não encontrado."));
        requireParty(authorId, freight.clienteId(), freight.prestadorId());
        boolean client = authorId == freight.clienteId();
        return new Confirmation(freight, client ? freight.clienteNome() : freight.prestadorNome(),
                client ? "Cliente" : "Prestador", repository.findCancellation(freightId).orElse(null));
    }

    @Transactional
    public void cancel(long freightId, long authorId, String justification) {
        String reason = justification == null ? "" : justification.replaceAll("(?U)^\\s+|\\s+$", "");
        if (reason.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Informe a justificativa do cancelamento.");
        // RF010 must serialize its transitions on this same row and recheck the state.
        var freight = repository.findForUpdate(freightId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Frete não encontrado."));
        requireParty(authorId, freight.clienteId(), freight.prestadorId());
        if (!freight.status().equals("AGUARDANDO_DESIGNACAO") && !freight.status().equals("PRONTO_INICIO")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este frete não pode ser cancelado: somente fretes que ainda não iniciaram podem ser cancelados.");
        }
        var cancelledAtUtc = repository.markCancelled(freightId);
        repository.addCancellationEvent(freightId, authorId, reason, cancelledAtUtc);
        long recipient = authorId == freight.clienteId() ? freight.prestadorId() : freight.clienteId();
        notificacoes.notificar(recipient, "Frete cancelado",
                "O frete FR-" + freightId + " foi cancelado. Consulte o motivo no registro do cancelamento.",
                freightId, null, cancelledAtUtc);
    }

    private void requireParty(long author, long client, long provider) {
        if (author != client && author != provider) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Somente o cliente ou o prestador responsável pode acessar o cancelamento deste frete.");
    }
}
