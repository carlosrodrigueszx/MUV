package com.muv.backend.execucao;

import com.muv.backend.repository.AuthRepository;
import java.security.Principal;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/fretes/{freteId}/execucao")
public class ExecucaoFreteController {
    private final AuthRepository contas;
    private final ExecucaoFreteService execucao;

    private final TransicaoFreteService transicao;

    public ExecucaoFreteController(AuthRepository contas, ExecucaoFreteService execucao,
                                   TransicaoFreteService transicao) {
        this.contas = contas;
        this.execucao = execucao;
        this.transicao = transicao;
    }

    @GetMapping
    public ResponseEntity<ExecucaoFreteService.Acompanhamento> consultar(
            @PathVariable long freteId, Principal principal) {
        var conta = contaAutenticada(principal);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(execucao.consultar(freteId, conta.id(), conta.nome()));
    }

    @PostMapping("/interrupcoes")
    public ResponseEntity<TransicaoFreteService.Resultado> interromper(
            @PathVariable long freteId, Principal principal, @RequestBody TransicaoFreteService.Comando comando) {
        var conta = contaAutenticada(principal);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(transicao.confirmar(freteId, conta.id(), RegrasExecucao.Acao.INTERRUPCAO, comando));
    }

    @PostMapping("/retomadas")
    public ResponseEntity<TransicaoFreteService.Resultado> retomar(
            @PathVariable long freteId, Principal principal, @RequestBody TransicaoFreteService.Comando comando) {
        var conta = contaAutenticada(principal);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(transicao.confirmar(freteId, conta.id(), RegrasExecucao.Acao.RETOMADA, comando));
    }

    @PostMapping("/encerramento-sem-conclusao")
    public ResponseEntity<TransicaoFreteService.Resultado> encerrar(
            @PathVariable long freteId, Principal principal, @RequestBody TransicaoFreteService.Comando comando) {
        var conta = contaAutenticada(principal);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(transicao.confirmar(freteId, conta.id(), RegrasExecucao.Acao.ENCERRAMENTO_SEM_CONCLUSAO, comando));
    }

    private AuthRepository.Account contaAutenticada(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return contas.findAccount(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
