package com.rea.sae_backend.dtos;

import com.rea.sae_backend.models.Alumno;
import com.rea.sae_backend.models.Escuela;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscuelaResponseDto {
    private Long id;
    private String nombre;
    private String cue;
    private Boolean asistenciaCompletada;

    public static EscuelaResponseDto fromEntity(Escuela escuela) {
        return fromEntity(escuela, null);
    }

    public static EscuelaResponseDto fromEntity(Escuela escuela, Boolean asistenciaCompletada) {
        if (escuela == null) {
            return null;
        }

        return EscuelaResponseDto.builder()
                .id(escuela.getId())
                .nombre(escuela.getNombre())
                .cue(escuela.getCue())
                .asistenciaCompletada(asistenciaCompletada)
                .build();
    }
}
