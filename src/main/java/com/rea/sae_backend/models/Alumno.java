package com.rea.sae_backend.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Escuela
 */
@Data
@Entity
@Table(name = "alumnos")
@NoArgsConstructor
@AllArgsConstructor
public class Alumno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellido;
    private String dni;
    private String nacimiento;
    private String localidad;

    @ManyToOne
    @JsonIgnoreProperties({"alumnos", "registros"})
    private Escuela escuela;

    @OneToMany(mappedBy = "alumno")
    @JsonIgnoreProperties("alumno")
    private List<RegistroAsistencia> registros;
}