package com.rea.sae_backend.dtos;

import com.rea.sae_backend.models.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDto {
    private Long id;
    private String nombre;
    private Long escuelaId;

    public static UsuarioResponseDto fromEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        return UsuarioResponseDto.builder()
                .id(usuario.getId())
                .nombre(usuario.getUsername())
                .escuelaId(usuario.getEscuela() != null ? usuario.getEscuela().getId() : null)
                .build();
    }
}
