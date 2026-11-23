package com.muv.backend.controller;

import com.muv.backend.dto.CentralNotificacoes;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.ConsultaNotificacoesService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {
    private final AuthRepository contas;
    private final ConsultaNotificacoesService servico;

    public NotificacaoController(AuthRepository contas, ConsultaNotificacoesService servico) {
        this.contas = contas;
        this.servico = servico;
    }

    @GetMapping
    public CentralNotificacoes listar(@RequestParam(defaultValue = "TODAS") String filtro,
                                      Principal principal) {
        if (!filtro.equals("TODAS") && !filtro.equals("NAO_LIDAS")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro de notificações inválido.");
        }
        return servico.listar(usuario(principal), filtro.equals("NAO_LIDAS"));
    }

    @PatchMapping("/{id}/leitura")
    public ResponseEntity<Void> marcarComoLida(@PathVariable long id, Principal principal) {
        servico.marcarComoLida(usuario(principal), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/leitura")
    public ResponseEntity<Void> marcarTodasComoLidas(Principal principal) {
        servico.marcarTodasComoLidas(usuario(principal));
        return ResponseEntity.noContent().build();
    }

    private long usuario(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return contas.findAccount(principal.getName()).map(AuthRepository.Account::id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
