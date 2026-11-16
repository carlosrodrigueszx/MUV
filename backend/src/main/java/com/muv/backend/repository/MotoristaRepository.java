package com.muv.backend.repository;

import com.muv.backend.model.Motorista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotoristaRepository extends JpaRepository<Motorista, Long> {
    java.util.Optional<Motorista> findByCnh(String cnh);
    boolean existsByCnh(String cnh);
    boolean existsByCpfIndice(String cpfIndice);
    java.util.Optional<Motorista> findByCpfIndice(String cpfIndice);
    @org.springframework.data.jpa.repository.Query("SELECT m FROM Motorista m, Usuario u WHERE m.usuarioId = u.id AND lower(trim(u.email)) = :email")
    java.util.Optional<Motorista> findByAccountEmail(@org.springframework.data.repository.query.Param("email") String email);
}