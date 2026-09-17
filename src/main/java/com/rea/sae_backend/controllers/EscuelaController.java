package com.rea.sae_backend.controllers;

import com.rea.sae_backend.dtos.EscuelaExcelImportResultDto;
import com.rea.sae_backend.dtos.EscuelaRequestDto;
import com.rea.sae_backend.dtos.EscuelaResponseDto;
import com.rea.sae_backend.dtos.EscuelaUpdateRequestDto;
import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.security.CurrentUserService;
import com.rea.sae_backend.services.EscuelaExcelService;
import com.rea.sae_backend.services.EscuelaService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<EscuelaResponseDto> list() {
        List<Escuela> escuelas = currentUserService.isAdmin()
            ? escuelaService.findAll()
            : escuelaService.findById(currentUserService.getEscuelaId()).map(List::of).orElseGet(List::of);
        return escuelas.stream()
                .map(escuelaService::toResponseDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (#id == @currentUserService.getEscuelaId())")
    public EscuelaResponseDto getById(@PathVariable Long id) {
        return escuelaService.findById(id)
            .map(escuelaService::toResponseDto)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Escuela no encontrada"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public EscuelaResponseDto create(@RequestBody EscuelaRequestDto escuelaDto) {
        return escuelaService.toResponseDto(escuelaService.create(escuelaDto));
    }

    @PostMapping(value = {"/cargar-excel", "/importar-excel"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EscuelaExcelImportResultDto> cargarExcel(@RequestParam("file") MultipartFile file) {
        EscuelaExcelImportResultDto resultado = escuelaExcelService.cargarEscuelasDesdeExcel(file);
        return ResponseEntity.ok(resultado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (#id == @currentUserService.getEscuelaId())")
    public EscuelaResponseDto update(@PathVariable Long id, @RequestBody EscuelaUpdateRequestDto escuelaDto) {
        return escuelaService.toResponseDto(escuelaService.update(id, escuelaDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        escuelaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/cerrar-asistencia")
    @PreAuthorize("hasRole('ADMIN') or (#id == @currentUserService.getEscuelaId())")
    public ResponseEntity<?> cerrarAsistencia(@PathVariable Long id) {
        Boolean asistenciaCerrada = escuelaService.cerrarAsistencia(id);
        return ResponseEntity.ok(asistenciaCerrada);
    }
}
