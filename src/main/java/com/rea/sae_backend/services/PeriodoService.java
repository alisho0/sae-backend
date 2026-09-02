package com.rea.sae_backend.services;

import com.rea.sae_backend.config.PeriodoConfig;
import com.rea.sae_backend.models.Periodo;
import com.rea.sae_backend.models.PeriodoActual;
import com.rea.sae_backend.repositories.PeriodoActualRepository;
import com.rea.sae_backend.repositories.PeriodoEscuelaRepository;
import com.rea.sae_backend.repositories.PeriodoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PeriodoService {
    private final PeriodoRepository periodoRepository;
    private final PeriodoActualRepository periodoActualRepository;
    private final PeriodoEscuelaRepository periodoEscuelaRepository;
    private final PeriodoConfig periodoConfig;

    /*
     * Devuelve el Periodo entidad correspondiente al valor mandado por parametros (ej. "10-2026").
     * Si no existe, lo crea y persiste en la BD.
     */
    @Transactional
    private Periodo getOrCreateByValor(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.isBlank()) {
            throw new IllegalArgumentException("El periodo no puede estar vacío");
        }
        return periodoRepository.findByValor(v)
                .orElseGet(() -> {
                    Periodo p = new Periodo();
                    p.setValor(v);
                    p.setCerrado(false);
                    return periodoRepository.save(p);
                });
    }

    /*
     Busca el periodo activo en la BD. Si no la encuentra, crea la instancia a partir del periodo activo
     que se crea por defecto con la variable de entorno.
     */
    @Transactional
    private Periodo getPeriodoActivo() {
        Optional<PeriodoActual> periodoActual = periodoActualRepository.findById(1L);
        if (periodoActual.isPresent() && periodoActual.get().getPeriodo().getValor() != null) {
            return periodoActual.get().getPeriodo();
        }
        String valorSemilla = periodoConfig.getPeriodoActivo();
        Periodo periodo = valorSemilla == null || valorSemilla.isBlank()
                ? null
                : getOrCreateByValor(valorSemilla);
        if (periodo == null) {
            throw new IllegalArgumentException("No hay un periodo activo configurado");
        }
        return periodo;
    }

    /*
        Trae el periodo si existe, sino lo crea. En caso de venir vacío el parámetro, llama a traer el periodo activo.
     */
    @Transactional
    public Periodo resolve ( String valor ) {
        if (valor == null || valor.isBlank()) {
            return getPeriodoActivo();
        }
        return getOrCreateByValor(valor);
    }
}
