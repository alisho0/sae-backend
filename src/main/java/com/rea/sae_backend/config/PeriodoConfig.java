package com.rea.sae_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PeriodoConfig {

    private final String periodoActivo;

    public PeriodoConfig(@Value("${app.periodo-activo:}") String periodoActivo) {
        this.periodoActivo = periodoActivo == null ? "" : periodoActivo.trim();
    }

    public String getPeriodoActivo() {
        return periodoActivo;
    }

    public String resolve(String periodo) {
        if (periodo == null || periodo.isBlank()) {
            return periodoActivo;
        }
        return periodo.trim();
    }
}