package com.rea.sae_backend.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "periodo", uniqueConstraints = {
        @UniqueConstraint(columnNames = "valor")
})
public class Periodo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String valor;
    private Boolean cerrado = false;
    private LocalDateTime fechaCierre;
}
