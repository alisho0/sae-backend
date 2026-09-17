package com.rea.sae_backend.security;

import com.rea.sae_backend.models.Usuario;
import com.rea.sae_backend.services.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UsuarioService usuarioService;

    public String getSubject() {
        Authentication auth = getAuthentication()
            .orElseThrow(() -> new AccessDeniedException("No autenticado"));
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }
        throw new AccessDeniedException("No autenticado con token OIDC");
    }

    public Long getEscuelaId() {
        return getAuthentication()
            .filter(auth -> auth instanceof JwtAuthenticationToken)
            .map(auth -> ((JwtAuthenticationToken) auth).getToken().getClaim("escuela_id"))
            .map(this::toLong)
            .orElseThrow(() -> new AccessDeniedException("Claim escuela_id ausente"));
    }

    public Set<String> getRoles() {
        return getAuthentication()
            .orElseThrow(() -> new AccessDeniedException("No autenticado"))
            .getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
            .collect(Collectors.toSet());
    }

    public boolean isAdmin() {
        return getRoles().contains("ADMIN");
    }

    public boolean isEscuelaAccesible(Long escuelaId) {
        return isAdmin() || getEscuelaId().equals(escuelaId);
    }

    /** Carga el registro de dominio vinculado al subject (para escuela u otros datos). */
    public Usuario getUsuarioDominio() {
        return usuarioService.findByKeycloakSubject(getSubject())
            .orElseThrow(() -> new AccessDeniedException("Usuario de dominio no encontrado"));
    }

    private Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        return null;
    }
}