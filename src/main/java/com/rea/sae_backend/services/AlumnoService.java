package com.rea.sae_backend.services;

import com.rea.sae_backend.dtos.AlumnoRequestDto;
import com.rea.sae_backend.dtos.AsistenciaRequestDto;
import com.rea.sae_backend.models.Alumno;
import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.repositories.AlumnoRepository;
import com.rea.sae_backend.repositories.EscuelaRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlumnoService {

    private final AlumnoRepository alumnoRepository;
    private final EscuelaRepository escuelaRepository;

    public Page<Alumno> findAll(Pageable pageable) {
        return alumnoRepository.findAll(pageable);
    }

    public Optional<Alumno> findById(Long id) {
        return alumnoRepository.findById(id);
    }

    public List<Alumno> findAllByEscuela(Long escuelaId) {
        return alumnoRepository.findByEscuelaId(escuelaId);
    }

    public Alumno create(AlumnoRequestDto alumno) {
        if (alumno.getCumpleAsistencia() == null) {
            alumno.setCumpleAsistencia(false);
        }
        if (alumno.getCreadoPorEscuela() == null) {
            alumno.setCreadoPorEscuela(false);
        }
        Alumno alumnoModel = new Alumno();
        alumnoModel.setNombre(alumno.getNombre());
        alumnoModel.setApellido(alumno.getApellido());
        alumnoModel.setCurso(alumno.getCurso());
        alumnoModel.setDni(alumno.getDni());
        alumnoModel.setLocalidad(alumno.getLocalidad());
        alumnoModel.setNacimiento(alumno.getNacimiento());
        alumnoModel.setCumpleAsistencia(alumno.getCumpleAsistencia());
        alumnoModel.setCreadoPorEscuela(alumno.getCreadoPorEscuela());

        Escuela escuela = escuelaRepository.findById(alumno.getEscuelaId())
            .orElseThrow(() -> new RuntimeException("Escuela no encontrada"));
        alumnoModel.setEscuela(escuela);

        return alumnoRepository.save(alumnoModel);
    }

    public Boolean updateAsistencia(Long id, boolean cumpleAsistencia) {
        Alumno a = alumnoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        
        a.setCumpleAsistencia(cumpleAsistencia);
        alumnoRepository.save(a);
        return a.getCumpleAsistencia();
    }

    public Alumno update(Long id, AlumnoRequestDto dto) {

    return alumnoRepository.findById(id)
        .map(existing -> {

            existing.setNombre(dto.getNombre());
            existing.setApellido(dto.getApellido());
            existing.setCurso(dto.getCurso());
            existing.setDni(dto.getDni());
            existing.setLocalidad(dto.getLocalidad());
            existing.setNacimiento(dto.getNacimiento());

            if (dto.getEscuelaId() != null) {
                Escuela escuela = escuelaRepository.findById(dto.getEscuelaId())
                    .orElseThrow(() ->
                        new RuntimeException("Escuela no encontrada")
                    );
                existing.setEscuela(escuela);
            }
            existing.setCumpleAsistencia(dto.getCumpleAsistencia() != null ? dto.getCumpleAsistencia() : existing.getCumpleAsistencia());
            existing.setCreadoPorEscuela(dto.getCreadoPorEscuela() != null ? dto.getCreadoPorEscuela() : existing.getCreadoPorEscuela());
            existing.setEditadoPorEscuela(dto.getEditadoPorEscuela() != null ? dto.getEditadoPorEscuela() : existing.getEditadoPorEscuela());

            return alumnoRepository.save(existing);
        })
        .orElseThrow(() ->
            new RuntimeException("Alumno no encontrado")
        );
    }

    public void delete(Long id) {
        alumnoRepository.deleteById(id);
    }
}
