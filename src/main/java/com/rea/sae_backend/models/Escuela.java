package com.rea.sae_backend.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Escuela
 */
@Data
@Entity
@Table(name = "escuelas")
@NoArgsConstructor
@AllArgsConstructor
public class Escuela {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String cue;

    private Boolean asistenciaCompletada;

    @OneToOne(mappedBy = "escuela")
    @JsonIgnoreProperties("escuela")
    private Usuario usuario;

    @OneToMany(mappedBy = "escuela")
    @JsonIgnoreProperties("escuela")
    private List<Alumno> alumnos;
}
