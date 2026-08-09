package com.rea.sae_backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscuelaExcelImportResultDto {
    private int totalFilas;
    private int exitosos;
    private int fallidos;
    private List<String> errores;
    private List<EscuelaResponseDto> escuelasCargadas;
}
