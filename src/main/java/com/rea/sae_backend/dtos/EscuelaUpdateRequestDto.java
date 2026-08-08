package com.rea.sae_backend.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscuelaUpdateRequestDto {
    private String nombre;
    private String cue;
    private Boolean asistenciaCompletada;
    private Long usuarioId;
    private List<Long> alumnoIds;
}
