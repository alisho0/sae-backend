package com.rea.sae_backend.controllers;

import com.rea.sae_backend.dtos.AlumnoResponseDto;
import com.rea.sae_backend.models.Alumno;
import com.rea.sae_backend.services.AlumnoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/alumnos")
public class AlumnoController {

    private final AlumnoService alumnoService;

    public AlumnoController(AlumnoService alumnoService) {
        this.alumnoService = alumnoService;
    }

    @GetMapping
    public List<AlumnoResponseDto> list() {
        return alumnoService.findAll().stream()
                .map(AlumnoResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public AlumnoResponseDto getById(@PathVariable Long id) {
        return alumnoService.findById(id)
            .map(AlumnoResponseDto::fromEntity)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alumno no encontrado"));
    }

    @PostMapping
    public AlumnoResponseDto create(@RequestBody Alumno alumno) {
        return AlumnoResponseDto.fromEntity(alumnoService.create(alumno));
    }

    @PutMapping("/{id}")
    public AlumnoResponseDto update(@PathVariable Long id, @RequestBody Alumno alumno) {
        return AlumnoResponseDto.fromEntity(alumnoService.update(id, alumno));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        alumnoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
