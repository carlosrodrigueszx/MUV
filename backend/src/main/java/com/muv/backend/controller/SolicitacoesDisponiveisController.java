package com.muv.backend.controller;

import com.muv.backend.repository.AuthRepository;
import com.muv.backend.repository.SolicitacoesDisponiveisRepository;
import com.muv.backend.service.SolicitacoesDisponiveisService;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/prestador/solicitacoes")
public class SolicitacoesDisponiveisController {
    private final AuthRepository contas;
    private final SolicitacoesDisponiveisService servico;

    public SolicitacoesDisponiveisController(AuthRepository contas, SolicitacoesDisponiveisService servico) {
        this.contas = contas;
        this.servico = servico;
    }

    @GetMapping
    public List<SolicitacoesDisponiveisRepository.Item> listar(
            @RequestParam(defaultValue = "TODAS") String modalidade,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) LocalDate data,
            Principal principal) {
        return servico.listar(prestador(principal), modalidade, cidade, estado, data);
    }

    @GetMapping("/perfil")
    public SolicitacoesDisponiveisRepository.Perfil perfil(Principal principal) {
        return servico.perfil(prestador(principal));
    }

    private long prestador(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return contas.findAccount(principal.getName())
                .map(AuthRepository.Account::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
