package com.muv.backend.dto;

import com.muv.backend.model.Motorista;
import com.muv.backend.model.VinculoMotorista;
import java.time.LocalDateTime;

public record MotoristaConsultaResponse(Long id, String nome, String telefone, String cnh,
        String categoriaCnh, String statusVerificacao, LocalDateTime dataVerificacao,
        String cpf, String nascimento, String genero, String email, String avatarUrl) {
    public static MotoristaConsultaResponse from(Motorista motorista) {
        return new MotoristaConsultaResponse(motorista.getId(), motorista.getNome(), motorista.getTelefone(),
                motorista.getCnh(), motorista.getCategoriaCnh(), null, null, null, null, null, null, null);
    }
    public static MotoristaConsultaResponse from(VinculoMotorista vinculo) {
        Motorista m = vinculo.getMotorista();
        return new MotoristaConsultaResponse(m.getId(), m.getNome(), m.getTelefone(), m.getCnh(),
                m.getCategoriaCnh(), vinculo.getStatusVerificacao(), vinculo.getDataVerificacao(), null, null, null, null, null);
    }
}
