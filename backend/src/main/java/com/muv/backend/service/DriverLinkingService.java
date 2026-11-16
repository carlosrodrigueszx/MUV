package com.muv.backend.service;

import com.muv.backend.dto.MotoristaVinculacaoPerfilResponse;
import com.muv.backend.model.*;
import com.muv.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DriverLinkingService {
    @Autowired private MotoristaRepository motoristaRepository;
    @Autowired private VinculoMotoristaRepository vinculoMotoristaRepository;
    @Autowired private PrestadorRepository prestadorRepository;
    @Autowired private PrestadorEmpresaRepository empresaRepository;
    @Autowired private AuthRepository accounts;
    @Autowired private DriverAccountService driverAccounts;

    public MotoristaVinculacaoPerfilResponse perfil(String email) {
        var account = accounts.findAccount(AuthService.normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        var empresa = empresaRepository.findById(account.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso exclusivo do gestor da empresa."));
        return new MotoristaVinculacaoPerfilResponse(empresa.getId(), empresa.getNomeFantasia());
    }

    public void autorizar(Long prestadorId, String email) {
        if (!perfil(email).id().equals(prestadorId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O vínculo pertence a outra organização.");
    }

    public List<VinculoMotorista> listarMotoristasPorPrestador(Long prestadorId) {
        return vinculoMotoristaRepository.findByIdPrestadorId(prestadorId);
    }

    public Motorista buscarMotorista(String cnh) {
        return motoristaRepository.findByCnh(cnh.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorista não encontrado."));
    }

    @Transactional
    public VinculoMotorista cadastrarEVincularMotorista(Long prestadorId, com.muv.backend.dto.MotoristaCadastroRequest dados) {
        Prestador prestador = prestadorRepository.findById(prestadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador não encontrado."));
        if (motoristaRepository.existsByCnh(dados.cnh()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CNH já cadastrada. Busque o motorista para vinculá-lo.");
        return salvarVinculo(prestador, driverAccounts.cadastrar(dados));
    }

    @Transactional
    public VinculoMotorista vincularMotorista(Long prestadorId, Long motoristaId) {
        Prestador prestador = prestadorRepository.findById(prestadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prestador não encontrado."));
        Motorista motorista = motoristaRepository.findById(motoristaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorista não encontrado."));
        if (vinculoMotoristaRepository.findByIdPrestadorIdAndIdMotoristaId(prestadorId, motoristaId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Motorista já vinculado à organização.");
        return salvarVinculo(prestador, motorista);
    }

    private VinculoMotorista salvarVinculo(Prestador prestador, Motorista motorista) {
        VinculoMotoristaId id = new VinculoMotoristaId();
        id.setPrestadorId(prestador.getId());
        id.setMotoristaId(motorista.getId());
        VinculoMotorista vinculo = new VinculoMotorista();
        vinculo.setId(id);
        vinculo.setPrestador(prestador);
        vinculo.setMotorista(motorista);
        vinculo.setStatusVerificacao("PENDENTE");
        return vinculoMotoristaRepository.save(vinculo);
    }

    @Transactional
    public VinculoMotorista atualizarVerificacao(Long prestadorId, Long motoristaId, String status, Long gestorId) {
        if (status == null || !Set.of("PENDENTE", "VERIFICADO", "REJEITADO").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de verificação inválido.");
        VinculoMotorista vinculo = vinculoMotoristaRepository.findByIdPrestadorIdAndIdMotoristaId(prestadorId, motoristaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vínculo não encontrado entre o prestador e o motorista."));
        vinculo.setStatusVerificacao(status);
        vinculo.setDataVerificacao(LocalDateTime.now());
        vinculo.setVerificadoPorId(gestorId);
        return vinculoMotoristaRepository.save(vinculo);
    }
}
