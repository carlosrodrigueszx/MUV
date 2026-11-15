package com.muv.backend.repository;

import com.muv.backend.model.PrestadorAutonomo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrestadorAutonomoRepository extends JpaRepository<PrestadorAutonomo, Long> {
}