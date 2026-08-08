package com.rea.sae_backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscuelaRequestDto {
    private Long id;
    private String nombre;
    private String cue;
    private Boolean asistenciaCompletada;
    private String usuarioNombre;
    private String usuarioPassword;
}
