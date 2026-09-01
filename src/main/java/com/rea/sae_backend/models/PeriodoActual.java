package com.rea.sae_backend.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "periodo_actual")
public class PeriodoActual {
    @Id
    private Long id = 1L;
    @ManyToOne
    private Periodo periodo;
}
