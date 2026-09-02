package com.rea.sae_backend.dtos;

import com.rea.sae_backend.models.Alumno;
import com.rea.sae_backend.models.RegistroAsistencia;
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
    private String periodo;
    private String nombre;
    private String apellido;
    private String curso;
    private String dni;
    private String nacimiento;
    private String localidad;
    private Boolean cumpleAsistencia;
    private Boolean creadoPorEscuela;
    private Boolean editadoPorEscuela;
    private String escuela;
    private Long escuelaId;

    public static AlumnoResponseDto fromEntity(RegistroAsistencia registro) {
        if (registro == null) {
            return null;
        }
        Alumno alumno = registro.getAlumno();
        return AlumnoResponseDto.builder()
                .id(registro.getId())
                .periodo(registro.getPeriodo().getValor())
                .nombre(alumno != null ? alumno.getNombre() : null)
                .apellido(alumno != null ? alumno.getApellido() : null)
                .curso(registro.getCurso())
                .dni(alumno != null ? alumno.getDni() : null)
                .nacimiento(alumno != null ? alumno.getNacimiento() : null)
                .localidad(alumno != null ? alumno.getLocalidad() : null)
                .cumpleAsistencia(registro.getCumpleAsistencia())
                .creadoPorEscuela(registro.getCreadoPorEscuela())
                .escuela(alumno != null && alumno.getEscuela() != null ? alumno.getEscuela().getNombre() : null)
                .escuelaId(alumno != null && alumno.getEscuela() != null ? alumno.getEscuela().getId() : null)
                .editadoPorEscuela(registro.getEditadoPorEscuela())
                .build();
    }
}