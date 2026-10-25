package com.rea.sae_backend.controllers;

import com.rea.sae_backend.dtos.AlumnoRequestDto;
import com.rea.sae_backend.dtos.AlumnoResponseDto;
import com.rea.sae_backend.dtos.AsistenciaRequestDto;
import com.rea.sae_backend.models.Alumno;
import com.rea.sae_backend.models.RegistroAsistencia;
import com.rea.sae_backend.security.CurrentUserService;
import com.rea.sae_backend.services.AlumnoService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import com.rea.sae_backend.dtos.ExcelImportResultDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/alumnos")
@RequiredArgsConstructor
public class AlumnoController {

    private final AlumnoService alumnoService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Page<AlumnoResponseDto> list( @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) Boolean cumpleAsistencia,
                                         @RequestParam(required = false) String dni,
                                         @RequestParam(required = false) Long escuelaId,
                                         @RequestParam(required = false) String periodo
                                        ) {

        Long escuelaEfectiva = currentUserService.isAdmin() ? escuelaId : currentUserService.getEscuelaId();
        Pageable pageable = PageRequest.of(page, size);
        return alumnoService
            .findAll(
                pageable, 
                cumpleAsistencia,
                dni,
                escuelaEfectiva,
                periodo
                )
            .map(AlumnoResponseDto::fromEntity);
    }

    @GetMapping("/{id}")
    public AlumnoResponseDto getById(@PathVariable Long id) {
        RegistroAsistencia registro = alumnoService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));
        verificarAccesoRegistro(registro);
        return AlumnoResponseDto.fromEntity(registro);
    }

    @GetMapping("/escuela/{id}")
    @PreAuthorize("hasRole('ADMIN') or (#id == @currentUserService.getEscuelaId())")
    public List<AlumnoResponseDto> listByEscuela(@PathVariable Long id,
                                                 @RequestParam(required = false) String periodo) {
        List<RegistroAsistencia> registros = alumnoService.findAllByEscuela(id, periodo);
        return registros.stream()
                .map(AlumnoResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @PutMapping("asistencia/{id}")
    public ResponseEntity<?> changeAsistencia(@PathVariable Long id, @RequestBody AsistenciaRequestDto asistencia) {
        try {
            RegistroAsistencia registro = alumnoService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));
            verificarAccesoRegistro(registro);
            Boolean updatedAsistencia = alumnoService.updateAsistencia(id, asistencia.isCumpleAsistencia());
            return ResponseEntity.ok(updatedAsistencia);
        } catch (AccessDeniedException e) {
            throw e;
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("ID de alumno inválido");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping
    public AlumnoResponseDto create(@RequestBody AlumnoRequestDto alumno) {
        if (!currentUserService.isAdmin()) {
            alumno.setEscuelaId(currentUserService.getEscuelaId());
            alumno.setCreadoPorEscuela(true);
        }
        return AlumnoResponseDto.fromEntity(alumnoService.create(alumno));
    }

    @PostMapping(value = {"/cargar-excel", "/importar-excel"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExcelImportResultDto> cargarExcel(@RequestParam("file") MultipartFile file,
                                                            @RequestParam(required = false) String periodo) {
        ExcelImportResultDto resultado = alumnoService.cargarAlumnosDesdeExcel(file, periodo, null);
        return ResponseEntity.ok(resultado);
    }

    @PutMapping("/{id}")
    public AlumnoResponseDto update(@PathVariable Long id, @RequestBody AlumnoRequestDto alumno) {
        RegistroAsistencia registro = alumnoService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));
        verificarAccesoRegistro(registro);
        if (!currentUserService.isAdmin()) {
            alumno.setEscuelaId(currentUserService.getEscuelaId());
        }
        return AlumnoResponseDto.fromEntity(alumnoService.update(id, alumno));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        alumnoService.findById(id).ifPresent(this::verificarAccesoRegistro);
        alumnoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void verificarAccesoRegistro(RegistroAsistencia registro) {
        if (currentUserService.isAdmin()) {
            return;
        }
        Alumno alumno = registro.getAlumno();
        if (alumno == null || alumno.getEscuela() == null
                || !currentUserService.getEscuelaId().equals(alumno.getEscuela().getId())) {
            throw new AccessDeniedException("No tiene permisos sobre la escuela de este alumno");
        }
    }
}