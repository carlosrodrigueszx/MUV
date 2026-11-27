package com.muv.backend.controller;

import com.muv.backend.service.EnvioSolicitacaoService;
import com.muv.backend.service.EnvioSolicitacaoService.ItinerarioEstimativa;
import com.muv.backend.service.EstimativaFreteService;
import com.muv.backend.service.EnvioSolicitacaoService.Envio;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @PostMapping("/encomendas")
    @ResponseStatus(HttpStatus.CREATED)
    public Criada enviarEncomenda(@Valid @RequestBody EnvioSolicitacaoService.EnvioEncomenda envio,
            Principal principal) {
        return new Criada(service.enviarEncomenda(principal.getName(), envio));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail rejeitada(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail camposInvalidos(MethodArgumentNotValidException error) {
        String detalhe = error.getBindingResult().getFieldErrors().stream()
                .map(campo -> rotulo(campo.getField()) + ": " + campo.getDefaultMessage())
                .distinct()
                .collect(java.util.stream.Collectors.joining(". "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
    }

    private static String rotulo(String campo) {
        return switch (campo) {
            case "partida" -> "Data e horário de partida";
            case "descricaoVolumes" -> "Quantidade e descrição dos volumes";
            case "pesoKg" -> "Peso total (kg)";
            case "alturaCm" -> "Altura (cm)";
            case "larguraCm" -> "Largura (cm)";
            case "profundidadeCm" -> "Profundidade (cm)";
            case "coleta.nome" -> "Nome do contato de coleta";
            case "coleta.telefone" -> "Telefone do contato de coleta";
            case "entrega.nome" -> "Nome do contato de entrega";
            case "entrega.telefone" -> "Telefone do contato de entrega";
            default -> campo.replace('.', ' ');
        };
    }

    public record Criada(long id) {}
}
