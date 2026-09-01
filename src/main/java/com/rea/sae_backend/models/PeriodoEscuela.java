package com.rea.sae_backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "periodo_escuela", uniqueConstraints = {
        @UniqueConstraint(name = "uk_escuela_periodo", columnNames = {"escuela_id", "periodo_id"} )
})
public class PeriodoEscuela {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private Periodo periodo;
    @ManyToOne
    private Escuela escuela;
    private Boolean cerrado = false;
    private LocalDateTime fechaCierre;
    @ManyToOne
    private Usuario cerradoPor;

}
