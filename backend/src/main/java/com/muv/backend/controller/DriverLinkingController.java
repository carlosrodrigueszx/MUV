package com.muv.backend.controller;

import com.muv.backend.dto.MotoristaCadastroRequest;
import com.muv.backend.dto.MotoristaConsultaResponse;
import com.muv.backend.dto.MotoristaVerificacaoRequest;
import com.muv.backend.dto.MotoristaVinculacaoPerfilResponse;
import com.muv.backend.dto.PrestadorConsultaResponse;
import com.muv.backend.service.DriverLinkingService;
import com.muv.backend.service.PrestadorConsultaService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/prestadores")
public class DriverLinkingController {

    @Autowired
    private DriverLinkingService driverLinkingService;

    @Autowired private com.muv.backend.service.DriverAccountService driverAccounts;

    @Value("${muv.demo.dispensar-verificacao:false}")
    private boolean dispensarVerificacao;

    private final PrestadorConsultaService prestadorConsultaService;

    public DriverLinkingController(PrestadorConsultaService prestadorConsultaService) {
        this.prestadorConsultaService = prestadorConsultaService;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/consulta")
    public ResponseEntity<List<PrestadorConsultaResponse>> buscarPrestadores(
            @RequestParam(required = false) String modalidade,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) Integer quantidadePassageiros,
            @RequestParam(required = false) Integer municipioCodigo) {
        return ResponseEntity.ok(prestadorConsultaService.buscar(
                modalidade, cidade, estado, tipo, quantidadePassageiros, municipioCodigo));
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/consulta/municipios")
    public List<PrestadorConsultaResponse.Municipio> listarMunicipios() {
        return prestadorConsultaService.listarMunicipios();
    }

    @GetMapping("/motoristas/perfil")
    public MotoristaVinculacaoPerfilResponse perfil(Principal principal) {
        return driverLinkingService.perfil(email(principal));
    }

    @GetMapping("/{prestadorId}/motoristas")
    public List<MotoristaConsultaResponse> listarMotoristas(
            @PathVariable Long prestadorId, @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "false") boolean aptos, Principal principal) {
        driverLinkingService.autorizar(prestadorId, email(principal));
        String query = search.trim().toLowerCase(java.util.Locale.ROOT);
        String document = query.replaceAll("[^0-9]", "");
        return driverLinkingService.listarMotoristasPorPrestador(prestadorId).stream()
                .filter(v -> !aptos || "VERIFICADO".equals(v.getStatusVerificacao())
                        || (dispensarVerificacao && "PENDENTE".equals(v.getStatusVerificacao())))
                .map(driverAccounts::resposta)
                .filter(m -> m.nome().toLowerCase(java.util.Locale.ROOT).contains(query)
                        || m.cnh().contains(query)
                        || (m.cpf() != null && !document.isEmpty()
                            && query.matches("[0-9. -]+") && m.cpf().contains(document)))
                .toList();
    }

    @GetMapping("/{prestadorId}/motoristas/busca")
    public MotoristaConsultaResponse buscarMotorista(@PathVariable Long prestadorId,
            @RequestParam String identifier, Principal principal) {
        driverLinkingService.autorizar(prestadorId, email(principal));
        return driverAccounts.resposta(driverAccounts.buscar(identifier));
    }

    @PostMapping("/{prestadorId}/motoristas")
    @ResponseStatus(HttpStatus.CREATED)
    public MotoristaConsultaResponse cadastrarMotorista(@PathVariable Long prestadorId,
            @Valid @RequestBody MotoristaCadastroRequest data,
            Principal principal) {
        driverLinkingService.autorizar(prestadorId, email(principal));
        return driverAccounts.resposta(driverLinkingService.cadastrarEVincularMotorista(prestadorId, data));
    }

    @PostMapping("/{prestadorId}/motoristas/{motoristaId}")
    @ResponseStatus(HttpStatus.CREATED)
    public MotoristaConsultaResponse vincularMotorista(@PathVariable Long prestadorId,
            @PathVariable Long motoristaId, Principal principal) {
        driverLinkingService.autorizar(prestadorId, email(principal));
        return driverAccounts.resposta(
                driverLinkingService.vincularMotorista(prestadorId, motoristaId));
    }

    @PatchMapping("/{prestadorId}/motoristas/{motoristaId}/verificacao")
    public MotoristaConsultaResponse atualizarVerificacao(@PathVariable Long prestadorId,
            @PathVariable Long motoristaId,
            @Valid @RequestBody MotoristaVerificacaoRequest data,
            Principal principal) {
        driverLinkingService.autorizar(prestadorId, email(principal));
        return driverAccounts.resposta(driverLinkingService.atualizarVerificacao(
                prestadorId, motoristaId, data.statusVerificacao(), prestadorId));
    }

    private static String email(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return principal.getName();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail rejeitado(ResponseStatusException error) {
        return ProblemDetail.forStatusAndDetail(error.getStatusCode(), error.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalido() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Confira os dados do motorista.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflito() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "E-mail, CPF, CNH ou vínculo já cadastrado.");
    }
}
