package com.muv.backend.service;

import com.muv.backend.repository.SolicitacoesDisponiveisRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SolicitacoesDisponiveisService {
    private final SolicitacoesDisponiveisRepository repositorio;

    public SolicitacoesDisponiveisService(SolicitacoesDisponiveisRepository repositorio) {
        this.repositorio = repositorio;
    }

    public SolicitacoesDisponiveisRepository.Perfil perfil(long prestador) {
        return repositorio.perfil(prestador).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }

    public List<SolicitacoesDisponiveisRepository.Item> listar(long prestador, String modalidade,
            String cidade, String estado, LocalDate data) {
        if (!repositorio.existePrestador(prestador)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        if (!List.of("TODAS", "PESSOA", "ENCOMENDA").contains(modalidade))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modalidade inválida.");
        if (estado != null && !estado.isBlank() && !estado.matches("[A-Za-z]{2}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido.");
        return repositorio.listar(prestador, modalidade, cidade, estado, data);
    }
}
