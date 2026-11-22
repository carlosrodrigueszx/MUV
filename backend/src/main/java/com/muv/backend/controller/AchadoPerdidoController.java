package com.muv.backend.controller;

import com.muv.backend.dto.AchadoPerdidoResponse;
import com.muv.backend.dto.AchadoPerdidoOpcoesResponse;
import com.muv.backend.dto.RegistrarAchadoRequest;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.AchadoPerdidoService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/achados-perdidos")
public class AchadoPerdidoController {
    private final AchadoPerdidoService service;
    private final AuthRepository accounts;

    public AchadoPerdidoController(AchadoPerdidoService service, AuthRepository accounts) {
        this.service = service;
        this.accounts = accounts;
    }

    @GetMapping
    public List<AchadoPerdidoResponse> listar(Principal principal) {
        return service.listar(usuario(principal));
    }

    @GetMapping("/opcoes")
    public AchadoPerdidoOpcoesResponse opcoes(Principal principal) {
        return service.opcoes(usuario(principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Created registrar(@Valid @RequestBody RegistrarAchadoRequest request, Principal principal) {
        return new Created(service.registrar(usuario(principal), request));
    }

    @PatchMapping("/{id}/devolucao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmarEntrega(@PathVariable long id, Principal principal) {
        service.confirmarEntrega(usuario(principal), id);
    }

    private long usuario(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return accounts.findAccount(principal.getName()).map(AuthRepository.Account::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public record Created(long id) {}

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail rejected(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }
}
