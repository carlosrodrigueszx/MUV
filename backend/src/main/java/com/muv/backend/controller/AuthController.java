package com.muv.backend.controller;

import com.muv.backend.dto.AuthRequests.*;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final AuthRepository accounts;

    public AuthController(AuthService auth, AuthRepository accounts) {
        this.auth = auth;
        this.accounts = accounts;
    }

    public record Csrf(String headerName, String token) {}
    public record Session(long id, String nome, String email) {}

    @GetMapping("/csrf")
    public Csrf csrf(CsrfToken csrf) {
        return new Csrf(csrf.getHeaderName(), csrf.getToken());
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody @Valid Registration data) {
        auth.register(data);
    }

    @GetMapping("/sessao")
    public Session session(Principal principal) {
        return accounts.findAccount(principal.getName())
                .map(a -> new Session(a.id(), a.nome(), a.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @PostMapping("/recuperacao")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void recovery(@RequestBody @Valid Recovery data) {
        auth.requestPasswordReset(data.email());
    }

    @PostMapping("/redefinicao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@RequestBody @Valid Reset data) {
        auth.resetPassword(data);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidFields(MethodArgumentNotValidException error) {
        String fields = error.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField()).distinct().reduce((a, b) -> a + ", " + b).orElse("formulário");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Confira os campos: " + fields + ".");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail rejected(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }
}
