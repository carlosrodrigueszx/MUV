package com.muv.backend.repository;

import com.muv.backend.model.VinculoMotorista;
import com.muv.backend.model.VinculoMotoristaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VinculoMotoristaRepository extends JpaRepository<VinculoMotorista, VinculoMotoristaId> {
    List<VinculoMotorista> findByIdPrestadorId(Long prestadorId);
    Optional<VinculoMotorista> findByIdPrestadorIdAndIdMotoristaId(Long prestadorId, Long motoristaId);
}