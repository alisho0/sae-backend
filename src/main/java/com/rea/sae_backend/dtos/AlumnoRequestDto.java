package com.rea.sae_backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlumnoRequestDto {

    private String nombre;
    private String apellido;
    private String curso;
    private String dni;
    private String nacimiento;
    private String localidad;
    private String periodo;
    private Boolean cumpleAsistencia;
    private Boolean creadoPorEscuela;
    private Boolean editadoPorEscuela;
    private Long escuelaId;
}