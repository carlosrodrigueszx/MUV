package com.muv.backend.controller;

import com.muv.backend.service.EnvioSolicitacaoService;
import com.muv.backend.service.EnvioSolicitacaoService.Envio;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cliente/solicitacoes")
public class EnvioSolicitacaoController {
    private final EnvioSolicitacaoService service;

    public EnvioSolicitacaoController(EnvioSolicitacaoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Criada enviar(@Valid @RequestBody Envio envio, Principal principal) {
        return new Criada(service.enviar(principal.getName(), envio));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail rejeitada(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }

    public record Criada(long id) {}
}
