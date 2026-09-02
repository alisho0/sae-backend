package com.rea.sae_backend.dtos;

import com.rea.sae_backend.models.Alumno;
import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.models.Periodo;
import com.rea.sae_backend.models.RegistroAsistencia;
import com.rea.sae_backend.models.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DtoBuilderTest {

    @Test
    void shouldMapEntitiesToResponseDtosUsingBuilder() {
        Escuela escuela = new Escuela();
        escuela.setId(7L);
        escuela.setNombre("Escuela Técnica");
        escuela.setCue("123456");
        escuela.setAsistenciaCompletada(true);

        Alumno alumno = new Alumno();
        alumno.setId(1L);
        alumno.setNombre("Ana");
        alumno.setApellido("Pérez");
        alumno.setDni("40111222");
        alumno.setLocalidad("San Martín");
        alumno.setEscuela(escuela);
        escuela.setAlumnos(List.of(alumno));

        Periodo periodo = new Periodo();
        periodo.setId(9L);
        periodo.setValor("09-2026");
        periodo.setCerrado(false);

        RegistroAsistencia registro = new RegistroAsistencia();
        registro.setId(5L);
        registro.setPeriodo(periodo);
        registro.setCurso("5to");
        registro.setCumpleAsistencia(true);
        registro.setCreadoPorEscuela(true);
        registro.setAlumno(alumno);
        alumno.setRegistros(List.of(registro));

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setUsername("admin");
        usuario.setPassword("secret");
        usuario.setEscuela(escuela);

        AlumnoResponseDto alumnoDto = AlumnoResponseDto.fromEntity(registro);
        EscuelaResponseDto escuelaDto = EscuelaResponseDto.fromEntity(escuela);
        UsuarioResponseDto usuarioDto = UsuarioResponseDto.fromEntity(usuario);

        assertEquals(5L, alumnoDto.getId());
        assertEquals("09-2026", alumnoDto.getPeriodo());
        assertEquals("Ana", alumnoDto.getNombre());
        assertEquals("5to", alumnoDto.getCurso());
        assertEquals(7L, alumnoDto.getEscuelaId());

        assertEquals(7L, escuelaDto.getId());
        assertEquals("Escuela Técnica", escuelaDto.getNombre());

        assertEquals(10L, usuarioDto.getId());
        assertEquals("admin", usuarioDto.getNombre());
        assertEquals(7L, usuarioDto.getEscuelaId());
    }
}