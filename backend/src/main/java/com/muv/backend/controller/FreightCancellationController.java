package com.muv.backend.controller;

import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.FreightCancellationService;
import com.muv.backend.service.FreightCancellationService.Confirmation;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/fretes/{id}/cancelamento")
public class FreightCancellationController {
    private final FreightCancellationService service;
    private final AuthRepository accounts;

    public FreightCancellationController(FreightCancellationService service, AuthRepository accounts) {
        this.service = service;
        this.accounts = accounts;
    }

    public record CancellationRequest(String justificativa) {}

    @GetMapping
    public Confirmation details(@PathVariable long id, Principal principal) {
        return service.details(id, author(principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable long id, Principal principal, @RequestBody CancellationRequest request) {
        service.cancel(id, author(principal), request.justificativa());
    }

    private long author(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return accounts.findAccount(principal.getName()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED)).id();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail rejected(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }
}
