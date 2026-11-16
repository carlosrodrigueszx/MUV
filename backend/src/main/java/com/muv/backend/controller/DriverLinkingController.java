package com.muv.backend.controller;

import com.muv.backend.dto.PrestadorConsultaResponse;
import com.muv.backend.model.Motorista;
import com.muv.backend.model.VinculoMotorista;
import com.muv.backend.service.DriverLinkingService;
import com.muv.backend.service.PrestadorConsultaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/prestadores")
@CrossOrigin(origins = "*")
public class DriverLinkingController {

    @Autowired
    private DriverLinkingService driverLinkingService;

    private final PrestadorConsultaService prestadorConsultaService;

    public DriverLinkingController(PrestadorConsultaService prestadorConsultaService) {
        this.prestadorConsultaService = prestadorConsultaService;
    }

    @GetMapping("/consulta")
    public ResponseEntity<List<PrestadorConsultaResponse>> buscarPrestadores(
            @RequestParam(required = false) String modalidade,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo) {
        return ResponseEntity.ok(prestadorConsultaService.buscar(modalidade, cidade, estado, tipo));
    }

    @GetMapping("/{prestadorId}/motoristas")
    public ResponseEntity<List<VinculoMotorista>> listarMotoristas(@PathVariable Long prestadorId) {
        return ResponseEntity.ok(driverLinkingService.listarMotoristasPorPrestador(prestadorId));
    }

    @PostMapping("/{prestadorId}/motoristas")
    public ResponseEntity<VinculoMotorista> cadastrarMotorista(
            @PathVariable Long prestadorId,
            @RequestBody Motorista motorista,
            @RequestParam Long gestorId) {
        return ResponseEntity.ok(driverLinkingService.cadastrarEVincularMotorista(prestadorId, motorista, gestorId));
    }

    @PatchMapping("/{prestadorId}/motoristas/{motoristaId}/verificacao")
    public ResponseEntity<VinculoMotorista> atualizarVerificacao(
            @PathVariable Long prestadorId,
            @PathVariable Long motoristaId,
            @RequestParam String statusVerificacao,
            @RequestParam Long gestorId) {
        return ResponseEntity.ok(driverLinkingService.atualizarVerificacao(prestadorId, motoristaId, statusVerificacao, gestorId));
    }
}