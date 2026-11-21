package com.muv.backend.controller;

import com.muv.backend.dto.Contratacao.Comparacao;
import com.muv.backend.dto.Contratacao.Detalhe;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.ContratacaoService;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cliente")
public class ContratacaoController {
    private final AuthRepository contas;
    private final ContratacaoService servico;

    public ContratacaoController(AuthRepository contas, ContratacaoService servico) {
        this.contas = contas;
        this.servico = servico;
    }

    @GetMapping("/solicitacoes/{id}/propostas")
    public Comparacao comparar(@PathVariable long id, Principal principal) {
        return servico.comparar(id, usuario(principal));
    }

    @GetMapping("/fretes/{id}")
    public Detalhe detalhe(@PathVariable long id, Principal principal) {
        return servico.detalhe(id, usuario(principal));
    }

    public record Confirmacao(long id) {}

    @PostMapping("/solicitacoes/{id}/propostas/{propostaId}/aceite")
    public ResponseEntity<Confirmacao> aceitar(@PathVariable long id, @PathVariable long propostaId,
                                               Principal principal) {
        var aceite = servico.aceitar(id, propostaId, usuario(principal));
        var resposta = new Confirmacao(aceite.id());
        return aceite.criado()
                ? ResponseEntity.created(URI.create("/api/cliente/fretes/" + aceite.id())).body(resposta)
                : ResponseEntity.ok(resposta);
    }

    private long usuario(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return contas.findAccount(principal.getName()).map(AuthRepository.Account::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
