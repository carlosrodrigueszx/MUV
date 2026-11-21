package com.muv.backend.controller;

import com.muv.backend.dto.DesignacaoFrete.Consulta;
import com.muv.backend.dto.DesignacaoFrete.ComandoDesignacao;
import com.muv.backend.dto.DesignacaoFrete.DesignacaoConfirmada;
import com.muv.backend.dto.DesignacaoFrete.ComandoSubstituicao;
import com.muv.backend.dto.DesignacaoFrete.SubstituicaoConfirmada;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.DesignacaoService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/prestador/fretes")
public class DesignacaoController {
    private final AuthRepository contas;
    private final DesignacaoService servico;

    public DesignacaoController(AuthRepository contas, DesignacaoService servico) {
        this.contas = contas;
        this.servico = servico;
    }

    @GetMapping("/{id}/designacao")
    public Consulta consultar(@PathVariable long id, Principal principal) {
        return servico.consultar(id, usuario(principal));
    }

    @PostMapping("/{id}/designacao")
    public DesignacaoConfirmada designar(
            @PathVariable long id,
            @RequestBody(required = false) ComandoDesignacao comando,
            Principal principal) {
        return servico.designar(id, usuario(principal), comando);
    }

    @PostMapping("/{id}/substituicoes")
    public SubstituicaoConfirmada substituir(
            @PathVariable long id,
            @RequestBody(required = false) ComandoSubstituicao comando,
            Principal principal) {
        return servico.substituir(id, usuario(principal), comando);
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
