package com.muv.backend.service;

import com.muv.backend.dto.PrestadorConsultaResponse;
import com.muv.backend.repository.PrestadorConsultaRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PrestadorConsultaService {
    private static final Set<String> MODALIDADES = Set.of("PESSOA", "ENCOMENDA");
    private static final Set<String> TIPOS = Set.of("EMPRESA", "AUTONOMO");

    private final PrestadorConsultaRepository repository;

    @org.springframework.beans.factory.annotation.Value("${muv.consulta.raio-km:50}")
    private double raioKm = 50;

    public PrestadorConsultaService(PrestadorConsultaRepository repository) {
        this.repository = repository;
    }

    public List<PrestadorConsultaResponse> buscar(
            String modalidade, String cidade, String estado, String tipo) {
        return buscar(modalidade, cidade, estado, tipo, null);
    }

    public List<PrestadorConsultaResponse> buscar(
            String modalidade, String cidade, String estado, String tipo, Integer quantidadePassageiros) {
        String modalidadeNormalizada = normalizeOption(modalidade, MODALIDADES, "modalidade");
        String tipoNormalizado = normalizeOption(tipo, TIPOS, "tipo");
        String cidadeNormalizada = normalizeText(cidade);
        String estadoNormalizado = normalizeText(estado);
        if (estadoNormalizado != null) {
            estadoNormalizado = estadoNormalizado.toUpperCase(Locale.ROOT);
            if (!estadoNormalizado.matches("[A-Z]{2}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O estado deve conter a sigla de dois caracteres.");
            }
        }
        if (quantidadePassageiros == null) {
            return repository.buscar(modalidadeNormalizada, cidadeNormalizada, estadoNormalizado, tipoNormalizado);
        }
        if (quantidadePassageiros < 1 || "ENCOMENDA".equals(modalidadeNormalizada)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe uma quantidade positiva de passageiros para a modalidade de pessoas.");
        }
        return repository.buscar("PESSOA", cidadeNormalizada, estadoNormalizado, tipoNormalizado, quantidadePassageiros);
    }

    public List<PrestadorConsultaResponse> buscar(String modalidade, String cidade, String estado,
            String tipo, Integer quantidadePassageiros, Integer municipioCodigo) {
        if (municipioCodigo == null) {
            return buscar(modalidade, cidade, estado, tipo, quantidadePassageiros);
        }
        String modalidadeNormalizada = normalizeOption(modalidade, MODALIDADES, "modalidade");
        if (modalidadeNormalizada == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione a modalidade de frete.");
        }
        if (quantidadePassageiros != null
                && (quantidadePassageiros < 1 || !"PESSOA".equals(modalidadeNormalizada))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe uma quantidade positiva de passageiros para a modalidade de pessoas.");
        }
        String tipoNormalizado = normalizeOption(tipo, TIPOS, "tipo");
        if (!repository.existeMunicipio(municipioCodigo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um município válido.");
        }
        return repository.buscarRegional(modalidadeNormalizada, tipoNormalizado, quantidadePassageiros,
                municipioCodigo, raioKm);
    }

    public List<PrestadorConsultaResponse.Municipio> listarMunicipios() {
        return repository.listarMunicipios();
    }

    private String normalizeOption(String value, Set<String> options, String field) {
        String normalized = normalizeText(value);
        if (normalized == null) return null;
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!options.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro de " + field + " inválido.");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
