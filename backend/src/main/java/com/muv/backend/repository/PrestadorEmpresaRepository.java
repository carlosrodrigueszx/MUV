package com.muv.backend.repository;

import com.muv.backend.model.PrestadorEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrestadorEmpresaRepository extends JpaRepository<PrestadorEmpresa, Long> {
}