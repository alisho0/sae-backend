package com.rea.sae_backend.controllers;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rea.sae_backend.services.ReporteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/escuela/{escuelaId}")
    public ResponseEntity<byte[]> descargarEscuela(
            @PathVariable Long escuelaId,
            @RequestParam(required = false) String periodo
    ) {
        try {
            byte[] archivo = reporteService.getAsistencia(escuelaId, periodo);
            return buildDownloadResponse(archivo);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/escuelas")
    public ResponseEntity<byte[]> descargarTodas(
            @RequestParam(required = false) String periodo
    ) {
        try {
            byte[] archivo = reporteService.getAsistencia(null, periodo);
            return buildDownloadResponse(archivo);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<byte[]> buildDownloadResponse(byte[] archivo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=asistencia.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    }
}