package com.rea.sae_backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registro mensual de un alumno: el mismo alumno tiene una fila por periodo.
 */
@Data
@Entity
@Table(
        name = "registro_asistencia",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_alumno_periodo", columnNames = {"alumno_id", "periodo_id"})
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAsistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties("registros")
    private Alumno alumno;

    @ManyToOne
    private Periodo periodo;

    private String curso;
    private Boolean cumpleAsistencia = false;
    private Boolean creadoPorEscuela = false;
    private Boolean editadoPorEscuela = false;
}