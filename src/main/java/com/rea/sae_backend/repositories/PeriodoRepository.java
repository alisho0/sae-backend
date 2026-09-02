package com.rea.sae_backend.repositories;

import com.rea.sae_backend.models.Periodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PeriodoRepository extends JpaRepository<Periodo, Long> {
    Optional<Periodo> findByValor(String valor);
}
