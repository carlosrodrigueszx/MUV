package com.muv.backend.service;

import com.muv.backend.model.Motorista;
import com.muv.backend.model.VinculoMotorista;
import com.muv.backend.model.VinculoMotoristaId;
import com.muv.backend.model.Prestador;
import com.muv.backend.repository.MotoristaRepository;
import com.muv.backend.repository.VinculoMotoristaRepository;
import com.muv.backend.repository.PrestadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DriverLinkingService {

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private VinculoMotoristaRepository vinculoMotoristaRepository;

    @Autowired
    private PrestadorRepository prestadorRepository;

    public List<VinculoMotorista> listarMotoristasPorPrestador(Long prestadorId) {
        return vinculoMotoristaRepository.findByIdPrestadorId(prestadorId);
    }

    @Transactional
    public VinculoMotorista cadastrarEVincularMotorista(Long prestadorId, Motorista motoristaDados, Long gestorId) {
        Prestador prestador = prestadorRepository.findById(prestadorId)
                .orElseThrow(() -> new RuntimeException("Prestador não encontrado."));

        Motorista motorista = motoristaRepository.save(motoristaDados);

        VinculoMotoristaId vinculoId = new VinculoMotoristaId();
        vinculoId.setPrestadorId(prestadorId);
        vinculoId.setMotoristaId(motorista.getId());

        VinculoMotorista vinculo = new VinculoMotorista();
        vinculo.setId(vinculoId);
        vinculo.setPrestador(prestador);
        vinculo.setMotorista(motorista);
        vinculo.setStatusVerificacao("PENDENTE");

        return vinculoMotoristaRepository.save(vinculo);
    }

    @Transactional
    public VinculoMotorista atualizarVerificacao(Long prestadorId, Long motoristaId, String statusVerificacao, Long gestorId) {
        VinculoMotorista vinculo = vinculoMotoristaRepository.findByIdPrestadorIdAndIdMotoristaId(prestadorId, motoristaId)
                .orElseThrow(() -> new RuntimeException("Vínculo não encontrado entre o prestador e o motorista."));

        vinculo.setStatusVerificacao(statusVerificacao); // Ex: 'VERIFICADO', 'PENDENTE', 'REJEITADO'
        vinculo.setDataVerificacao(LocalDateTime.now());
        vinculo.setVerificadoPorId(gestorId);

        return vinculoMotoristaRepository.save(vinculo);
    }
}