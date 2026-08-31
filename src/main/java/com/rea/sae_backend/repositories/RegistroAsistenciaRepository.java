package com.rea.sae_backend.repositories;

import com.rea.sae_backend.models.RegistroAsistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RegistroAsistenciaRepository
        extends JpaRepository<RegistroAsistencia, Long>, JpaSpecificationExecutor<RegistroAsistencia> {
    Optional<RegistroAsistencia> findByAlumnoIdAndPeriodo(Long alumnoId, String periodo);
}