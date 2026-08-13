package com.rea.sae_backend.dtos;

import com.rea.sae_backend.models.Alumno;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlumnoResponseDto {
    private Long id;
    private String nombre;
    private String apellido;
    private String curso;
    private String dni;
    private String nacimiento;
    private String localidad;
    private Boolean cumpleAsistencia;
    private Boolean creadoPorEscuela;
    private Boolean editadoPorEscuela;
    private Long escuelaId;

    public static AlumnoResponseDto fromEntity(Alumno alumno) {
        if (alumno == null) {
            return null;
        }

        return AlumnoResponseDto.builder()
                .id(alumno.getId())
                .nombre(alumno.getNombre())
                .apellido(alumno.getApellido())
                .curso(alumno.getCurso())
                .dni(alumno.getDni())
                .nacimiento(alumno.getNacimiento())
                .localidad(alumno.getLocalidad())
                .cumpleAsistencia(alumno.getCumpleAsistencia())
                .creadoPorEscuela(alumno.getCreadoPorEscuela())
                .escuelaId(alumno.getEscuela() != null ? alumno.getEscuela().getId() : null)
                .editadoPorEscuela(alumno.getEditadoPorEscuela())
                .build();
    }
}
