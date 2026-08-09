package com.rea.sae_backend.repositories;

import com.rea.sae_backend.models.Escuela;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EscuelaRepository extends JpaRepository<Escuela, Long> {
    Optional<Escuela> findByCue(String cue);
}
