package com.muv.backend.controller;

import com.muv.backend.dto.FreightHistoryProfile;
import com.muv.backend.dto.FreightHistoryResponse;
import com.muv.backend.service.FreightHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/historico-fretes")
@CrossOrigin(origins = "*")
public class FreightHistoryController {

    private final FreightHistoryService service;

    public FreightHistoryController(FreightHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<FreightHistoryResponse> list(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam FreightHistoryProfile perfil,
            @RequestParam(required = false) String modalidade,
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth periodo) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A conta autenticada é obrigatória.");
        }
        try {
            return service.list(userId, perfil, modalidade, situacao, periodo);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

}
