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

import com.rea.sae_backend.dtos.ExcelImportResultDto;
import com.rea.sae_backend.services.AlumnoExcelService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/alumnos")
public class AlumnoController {

    private final AlumnoService alumnoService;
    private final AlumnoExcelService alumnoExcelService;

    public AlumnoController(AlumnoService alumnoService, AlumnoExcelService alumnoExcelService) {
        this.alumnoService = alumnoService;
        this.alumnoExcelService = alumnoExcelService;
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

    @PostMapping(value = {"/cargar-excel", "/importar-excel"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExcelImportResultDto> cargarExcel(@RequestParam("file") MultipartFile file) {
        ExcelImportResultDto resultado = alumnoExcelService.cargarAlumnosDesdeExcel(file);
        return ResponseEntity.ok(resultado);
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
