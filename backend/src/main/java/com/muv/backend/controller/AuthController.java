package com.muv.backend.controller;

import com.muv.backend.dto.AuthRequests.*;
import com.muv.backend.repository.AuthRepository;
import com.muv.backend.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
    public record Session(long id, String nome, String email, boolean podeAtivarAutonomo,
                          boolean prestador, boolean motorista, boolean passageiro, boolean empresa) {}

    @GetMapping("/csrf")
    public Csrf csrf(CsrfToken csrf) {
        return new Csrf(csrf.getHeaderName(), csrf.getToken());
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody @Valid Registration data) {
        auth.register(data);
    }

    @PostMapping("/cadastro/empresa")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerCompany(@RequestBody @Valid CompanyRegistration data) {
        auth.registerCompany(data);
    }

    @PostMapping("/cadastro/autonomo")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerIndependent(@RequestBody @Valid IndependentRegistration data) {
        auth.registerIndependent(data);
    }

    @GetMapping("/sessao")
    public Session session(Principal principal) {
        return accounts.findAccount(principal.getName())
                .map(a -> {
                    var profiles = accounts.profiles(a.id());
                    return new Session(a.id(), a.nome(), a.email(), profiles.podeAtivarAutonomo(),
                            profiles.prestador(), profiles.motorista(), profiles.passageiro(), profiles.empresa());
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @PostMapping("/perfil/autonomo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateIndependent(@RequestBody @Valid IndependentActivation data, Principal principal) {
        auth.activateIndependent(principal.getName(), data);
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
                .map(field -> fieldLabel(field.getField()))
                .distinct()
                .collect(Collectors.joining(", "));
        String detail = fields.isEmpty() ? "Confira os dados do formulário." : "Confira os campos: " + fields + ".";
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadableForm() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Os dados do formulário estão incompletos ou em formato inválido.");
    }

    private static String fieldLabel(String field) {
        return switch (field.substring(field.lastIndexOf('.') + 1)) {
            case "nome" -> "nome completo";
            case "nascimento" -> "data de nascimento";
            case "cpf" -> "CPF";
            case "genero" -> "gênero / identidade";
            case "email" -> "e-mail";
            case "telefone" -> "telefone";
            case "senha" -> "senha";
            case "confirmarSenha" -> "confirmação de senha";
            case "razaoSocial" -> "razão social";
            case "nomeFantasia" -> "nome fantasia";
            case "cnpj" -> "CNPJ";
            case "cnh" -> "CNH";
            case "categoriaCnh" -> "categoria da CNH";
            case "aceitouTermos" -> "aceite dos termos";
            case "declarouConformidade" -> "declaração de conformidade";
            case "token" -> "link de recuperação";
            default -> "dados da conta";
        };
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail rejected(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }
}
