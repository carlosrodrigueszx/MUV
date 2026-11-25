package com.muv.backend.controller;

import com.muv.backend.service.EnvioSolicitacaoService;
import com.muv.backend.service.EnvioSolicitacaoService.ItinerarioEstimativa;
import com.muv.backend.service.EstimativaFreteService;
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

    private final EstimativaFreteService estimativas;

    public EnvioSolicitacaoController(EnvioSolicitacaoService service, EstimativaFreteService estimativas) {
        this.service = service;
        this.estimativas = estimativas;
    }

    @PostMapping(params = "estimativa")
    public EstimativaFreteService.Estimativa estimar(@Valid @RequestBody ItinerarioEstimativa itinerario, Principal principal) {
        service.consultarEnvioPessoa(principal.getName(), null);
        return estimativas.calcularParaEnderecos("PESSOA", itinerario.enderecos());
    }

    @PostMapping(params = "!estimativa")
    @ResponseStatus(HttpStatus.CREATED)
    public Criada enviar(@Valid @RequestBody Envio envio, Principal principal) {
        Long anterior = service.consultarEnvioPessoa(principal.getName(), envio.chave());
        if (anterior != null) return new Criada(anterior);
        var itinerario = new ItinerarioEstimativa(envio.origem(), envio.destino(), envio.paradas());
        var estimativa = estimativas.calcularParaEnderecos("PESSOA", itinerario.enderecos());
        return new Criada(service.enviar(principal.getName(), envio, estimativa));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail rejeitada(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }

    public record Criada(long id) {}
}
