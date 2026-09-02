package com.rea.sae_backend.repositories;

import com.rea.sae_backend.models.Periodo;
import com.rea.sae_backend.models.PeriodoActual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PeriodoActualRepository extends JpaRepository<PeriodoActual, Long> {
}
