package com.muv.backend.controller;

import com.muv.backend.dto.DesignacaoFrete.ConsultaCliente;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.DesignacaoService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cliente/fretes")
public class ClienteDesignacaoController {
    private final AuthRepository contas;
    private final DesignacaoService servico;

    public ClienteDesignacaoController(AuthRepository contas, DesignacaoService servico) {
        this.contas = contas;
        this.servico = servico;
    }

    @GetMapping("/{id}/designacao")
    public ConsultaCliente consultar(@PathVariable long id, Principal principal) {
        return servico.consultarCliente(id, usuario(principal));
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
