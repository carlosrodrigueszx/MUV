package com.muv.backend.controller;

import com.muv.backend.dto.DesignacaoFrete.AcordoRespondido;
import com.muv.backend.dto.DesignacaoFrete.RespostaAcordo;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.DesignacaoService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cliente/fretes")
public class AcordoController {
    private final AuthRepository contas;
    private final DesignacaoService servico;

    public AcordoController(AuthRepository contas, DesignacaoService servico) {
        this.contas = contas;
        this.servico = servico;
    }

    @PostMapping("/{freteId}/acordos/{acordoId}/resposta")
    public AcordoRespondido responder(
            @PathVariable long freteId,
            @PathVariable long acordoId,
            @RequestBody RespostaAcordo resposta,
            Principal principal) {
        return servico.responderAcordo(
                freteId, acordoId, usuario(principal), resposta.aceitar());
    }

    private long usuario(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return contas.findAccount(principal.getName())
                .map(AuthRepository.Account::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
