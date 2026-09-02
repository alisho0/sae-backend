package com.rea.sae_backend.dtos;

import com.rea.sae_backend.models.Periodo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodoResponseDto {
    private Long id;
    private String valor;
    private Boolean cerrado;

    public static PeriodoResponseDto fromEntity(Periodo periodo) {
        if (periodo == null) return null;
        return new PeriodoResponseDto(periodo.getId(), periodo.getValor(), periodo.getCerrado());
    }
}
