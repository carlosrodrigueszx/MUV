package com.muv.backend.dto;

import java.util.List;

public record AchadoPerdidoOpcoesResponse(List<Veiculo> veiculos, List<Frete> fretes) {
    public record Veiculo(long id, long prestadorId, String marca, String modelo, String placa) {}
    public record Frete(long id, long prestadorId, String status) {}
}
