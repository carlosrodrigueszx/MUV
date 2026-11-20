package com.muv.backend.controller;

import com.muv.backend.dto.FreightHistoryProfile;
import com.muv.backend.dto.FreightHistoryResponse;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.FreightHistoryService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/historico-fretes")
public class FreightHistoryController {

    private final FreightHistoryService service;
    private final AuthRepository accounts;

    public FreightHistoryController(FreightHistoryService service, AuthRepository accounts) {
        this.service = service;
        this.accounts = accounts;
    }

    @GetMapping("/perfis")
    public List<FreightHistoryProfile> profiles(Principal principal) {
        return service.availableProfiles(authenticatedUserId(principal));
    }

    @GetMapping
    public List<FreightHistoryResponse> list(
            Principal principal,
            @RequestParam FreightHistoryProfile perfil,
            @RequestParam(required = false) String modalidade,
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo) {
        long userId = authenticatedUserId(principal);
        try {
            return service.list(userId, perfil, modalidade, situacao, periodo);
        } catch (AccessDeniedException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private long authenticatedUserId(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A conta autenticada é obrigatória.");
        }
        return accounts.findAccount(principal.getName())
                .map(AuthRepository.Account::id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "A conta autenticada não foi encontrada."));
    }

}
