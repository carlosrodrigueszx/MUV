package com.muv.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record PrestadorConsultaResponse(
        Long id,
        String nome,
        String tipo,
        List<String> modalidadesAutorizadas,
        List<Regiao> regioesAtendidas,
        List<Veiculo> veiculosDisponiveis,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        Cobertura cobertura) {

    public record Regiao(String cidade, String estado) {}
    public record Municipio(int codigo, String cidade, String estado) {}
    public record Cobertura(String nivel, Municipio municipio) {}

    public record Veiculo(
            Long id,
            String marca,
            String modelo,
            int ano,
            String modalidade,
            int capacidadePassageiros,
            BigDecimal capacidadeCargaKg) {}
}
