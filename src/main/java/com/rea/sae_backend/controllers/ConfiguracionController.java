package com.rea.sae_backend.controllers;

import com.rea.sae_backend.dtos.PeriodoResponseDto;
import com.rea.sae_backend.models.Periodo;
import com.rea.sae_backend.services.PeriodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final PeriodoService periodoService;

    @GetMapping("/periodo")
    public PeriodoResponseDto getPeriodoActivo() {
        return PeriodoResponseDto.fromEntity(periodoService.getPeriodoActivo());
    }

    @PutMapping("/periodo")
    public PeriodoResponseDto setPeriodoActivo(@RequestBody Map<String, String> body) {
        String valor = body.get("valor");
        if (valor == null || valor.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "El campo 'valor' es obligatorio (ej. \"11-2026\")"
            );
        }
        Periodo periodo = periodoService.setPeriodoActivo(valor);
        return PeriodoResponseDto.fromEntity(periodo);
    }
}
