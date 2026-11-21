package com.muv.backend.controller;

import com.muv.backend.dto.AvaliacaoSolicitacao;
import com.muv.backend.dto.RespostaPrestador;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.PropostaService;
import com.muv.backend.service.PercursoService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/prestador/solicitacoes/{id}")
public class PropostaController {
    private final AuthRepository contas;
    private final PropostaService servico;

    private final PercursoService percurso;

    public PropostaController(AuthRepository contas, PropostaService servico, PercursoService percurso) {
        this.percurso = percurso;
        this.contas = contas;
        this.servico = servico;
    }

    @GetMapping("/avaliacao")
    public AvaliacaoSolicitacao avaliar(@PathVariable long id, Principal principal) {
        return servico.avaliar(id, usuario(principal));
    }

    @GetMapping("/avaliacao/percurso")
    public PercursoService.Percurso percurso(@PathVariable long id, Principal principal) {
        return percurso.consultar(servico.avaliar(id, usuario(principal)));
    }

    public record Confirmacao(long id) {}

    @PostMapping("/propostas")
    @ResponseStatus(HttpStatus.CREATED)
    public Confirmacao responder(@PathVariable long id, @RequestBody RespostaPrestador resposta, Principal principal) {
        return new Confirmacao(servico.responder(id, usuario(principal), resposta));
    }

    public record Recusa(String justificativa) {}

    @PostMapping("/recusa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recusar(@PathVariable long id, @RequestBody Recusa recusa, Principal principal) {
        servico.recusar(id, usuario(principal), recusa.justificativa());
    }

    private long usuario(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return contas.findAccount(principal.getName()).map(AuthRepository.Account::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
