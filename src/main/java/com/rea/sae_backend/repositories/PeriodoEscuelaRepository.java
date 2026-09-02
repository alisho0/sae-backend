package com.rea.sae_backend.repositories;

import com.rea.sae_backend.models.PeriodoEscuela;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PeriodoEscuelaRepository extends JpaRepository<PeriodoEscuela, Long> {
    Optional<PeriodoEscuela> findByEscuelaIdAndPeriodoId(Long escuelaId, Long periodoId);
}
