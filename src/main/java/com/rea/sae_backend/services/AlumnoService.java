package com.rea.sae_backend.services;

import com.rea.sae_backend.models.Alumno;
import com.rea.sae_backend.repositories.AlumnoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AlumnoService {

    private final AlumnoRepository alumnoRepository;

    public AlumnoService(AlumnoRepository alumnoRepository) {
        this.alumnoRepository = alumnoRepository;
    }

    public List<Alumno> findAll() {
        return alumnoRepository.findAll();
    }

    public Optional<Alumno> findById(Long id) {
        return alumnoRepository.findById(id);
    }

    public Alumno create(Alumno alumno) {
        if (alumno.getCumpleAsistencia() == null) {
            alumno.setCumpleAsistencia(false);
        }
        if (alumno.getCreadoPorEscuela() == null) {
            alumno.setCreadoPorEscuela(false);
        }
        return alumnoRepository.save(alumno);
    }

    public Alumno update(Long id, Alumno alumnoDetails) {
        return alumnoRepository.findById(id)
            .map(existing -> {
                existing.setNombre(alumnoDetails.getNombre());
                existing.setApellido(alumnoDetails.getApellido());
                existing.setCurso(alumnoDetails.getCurso());
                existing.setDni(alumnoDetails.getDni());
                existing.setLocalidad(alumnoDetails.getLocalidad());
                existing.setCumpleAsistencia(
                    alumnoDetails.getCumpleAsistencia() == null ? false : alumnoDetails.getCumpleAsistencia());
                existing.setCreadoPorEscuela(
                    alumnoDetails.getCreadoPorEscuela() == null ? false : alumnoDetails.getCreadoPorEscuela());
                existing.setEscuela(alumnoDetails.getEscuela());
                return alumnoRepository.save(existing);
            })
            .orElseGet(() -> {
                alumnoDetails.setId(id);
                if (alumnoDetails.getCumpleAsistencia() == null) {
                    alumnoDetails.setCumpleAsistencia(false);
                }
                if (alumnoDetails.getCreadoPorEscuela() == null) {
                    alumnoDetails.setCreadoPorEscuela(false);
                }
                return alumnoRepository.save(alumnoDetails);
            });
    }

    public void delete(Long id) {
        alumnoRepository.deleteById(id);
    }
}
