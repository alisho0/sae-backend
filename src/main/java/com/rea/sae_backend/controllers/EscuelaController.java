package com.rea.sae_backend.controllers;

import com.rea.sae_backend.dtos.EscuelaExcelImportResultDto;
import com.rea.sae_backend.dtos.EscuelaRequestDto;
import com.rea.sae_backend.dtos.EscuelaResponseDto;
import com.rea.sae_backend.dtos.EscuelaUpdateRequestDto;
import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.services.EscuelaExcelService;
import com.rea.sae_backend.services.EscuelaService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/escuelas")
@RequiredArgsConstructor
public class EscuelaController {

    private final EscuelaService escuelaService;
    private final EscuelaExcelService escuelaExcelService;

    @GetMapping
    public List<EscuelaResponseDto> list() {
        return escuelaService.findAll().stream()
                .map(EscuelaResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public EscuelaResponseDto getById(@PathVariable Long id) {
        return escuelaService.findById(id)
            .map(EscuelaResponseDto::fromEntity)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escuela no encontrada"));
    }

    @PostMapping
    public EscuelaResponseDto create(@RequestBody EscuelaRequestDto escuelaDto) {
        return EscuelaResponseDto.fromEntity(escuelaService.create(escuelaDto));
    }

    @PostMapping(value = {"/cargar-excel", "/importar-excel"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EscuelaExcelImportResultDto> cargarExcel(@RequestParam("file") MultipartFile file) {
        EscuelaExcelImportResultDto resultado = escuelaExcelService.cargarEscuelasDesdeExcel(file);
        return ResponseEntity.ok(resultado);
    }

    @PutMapping("/{id}")
    public EscuelaResponseDto update(@PathVariable Long id, @RequestBody EscuelaUpdateRequestDto escuelaDto) {
        return EscuelaResponseDto.fromEntity(escuelaService.update(id, escuelaDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        escuelaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/cerrar-asistencia")
    public ResponseEntity<?> cerrarAsistencia(@PathVariable Long id) {
        Boolean asistenciaCerrada = escuelaService.cerrarAsistencia(id);
        return ResponseEntity.ok(asistenciaCerrada);
    }
}
