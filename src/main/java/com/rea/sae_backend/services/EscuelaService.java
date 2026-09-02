package com.rea.sae_backend.services;

import com.rea.sae_backend.dtos.EscuelaRequestDto;
import com.rea.sae_backend.dtos.EscuelaResponseDto;
import com.rea.sae_backend.dtos.EscuelaUpdateRequestDto;
import com.rea.sae_backend.models.Alumno;
import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.models.Periodo;
import com.rea.sae_backend.models.PeriodoEscuela;
import com.rea.sae_backend.models.RegistroAsistencia;
import com.rea.sae_backend.models.Role;
import com.rea.sae_backend.models.Usuario;
import com.rea.sae_backend.repositories.EscuelaRepository;
import com.rea.sae_backend.repositories.PeriodoEscuelaRepository;
import com.rea.sae_backend.repositories.RegistroAsistenciaRepository;
import com.rea.sae_backend.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EscuelaService {

    private final EscuelaRepository escuelaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroAsistenciaRepository registroRepository;
    private final PeriodoEscuelaRepository periodoEscuelaRepository;
    private final PeriodoService periodoService;
    private final UsuarioService usuarioService;

    public List<Escuela> findAll() {
        return escuelaRepository.findAll();
    }

    public Optional<Escuela> findById(Long id) {
        return escuelaRepository.findById(id);
    }

    @Transactional
    public Escuela create(EscuelaRequestDto escuela) {
        if (escuela.getUsuarioNombre() == null || escuela.getUsuarioNombre().isBlank()
                || escuela.getUsuarioPassword() == null || escuela.getUsuarioPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe proporcionar nombre y password del usuario director");
        }

        Escuela escuelaEntity = new Escuela();
        escuelaEntity.setNombre(escuela.getNombre() != null ? escuela.getNombre() : "");
        escuelaEntity.setCue(escuela.getCue() != null ? escuela.getCue() : "");
        escuelaEntity = escuelaRepository.save(escuelaEntity);

        Usuario usuario = new Usuario();
        usuario.setUsername(escuela.getUsuarioNombre());
        usuario.setPassword(escuela.getUsuarioPassword());
        usuario.setEscuela(escuelaEntity);
        usuario.setRole(Role.DIRECTOR);
        usuario = usuarioService.create(usuario);

        escuelaEntity.setUsuario(usuario);
        return escuelaEntity;
    }

    public Escuela update(Long id, EscuelaUpdateRequestDto escuelaDetails) {
        return escuelaRepository.findById(id)
            .map(existing -> {
                existing.setNombre(escuelaDetails.getNombre() != null ? escuelaDetails.getNombre() : existing.getNombre());
                existing.setCue(escuelaDetails.getCue() != null ? escuelaDetails.getCue() : existing.getCue());

                List<Long> alumnoIds = escuelaDetails.getAlumnoIds();
                if (alumnoIds != null) {
                    List<Alumno> alumnos = registroRepository.findAllById(alumnoIds).stream()
                            .map(RegistroAsistencia::getAlumno)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                    existing.setAlumnos(alumnos);
                }
                Usuario usuario = usuarioRepository.findById(escuelaDetails.getUsuarioId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
                existing.setUsuario(usuario);
                return escuelaRepository.save(existing);
            })
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escuela no encontrada"));
    }

    public void delete(Long id) {
        Escuela escuela = escuelaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escuela no encontrada"));

        if (escuela.getUsuario() != null) {
            usuarioRepository.deleteById(escuela.getUsuario().getId());
        }

        escuelaRepository.deleteById(id);
    }

    @Transactional
    public Boolean cerrarAsistencia(Long id) {
        Escuela escuela = escuelaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escuela no encontrada"));

        Periodo periodoActivo = periodoService.getPeriodoActivo();

        PeriodoEscuela periodoEscuela = periodoEscuelaRepository
                .findByEscuelaIdAndPeriodoId(escuela.getId(), periodoActivo.getId())
                .orElseGet(() -> {
                    PeriodoEscuela pe = new PeriodoEscuela();
                    pe.setEscuela(escuela);
                    pe.setPeriodo(periodoActivo);
                    pe.setCerrado(false);
                    return pe;
                });

        periodoEscuela.setCerrado(true);
        periodoEscuela.setFechaCierre(LocalDateTime.now());
        periodoEscuelaRepository.save(periodoEscuela);

        return periodoEscuela.getCerrado();
    }

    public Boolean isAsistenciaCompletada(Long escuelaId) {
        Periodo periodoActivo = periodoService.getPeriodoActivo();
        return periodoEscuelaRepository
                .findByEscuelaIdAndPeriodoId(escuelaId, periodoActivo.getId())
                .map(PeriodoEscuela::getCerrado)
                .orElse(false);
    }

    public EscuelaResponseDto toResponseDto(Escuela escuela) {
        return EscuelaResponseDto.fromEntity(escuela, isAsistenciaCompletada(escuela.getId()));
    }
}
