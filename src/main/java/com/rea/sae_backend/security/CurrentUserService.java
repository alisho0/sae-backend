package com.rea.sae_backend.security;

import com.rea.sae_backend.models.Escuela;
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

    public String getPreferredUsername() {
        Authentication auth = getAuthentication()
            .orElseThrow(() -> new AccessDeniedException("No autenticado"));
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
            return jwtAuth.getToken().getSubject();
        }
        throw new AccessDeniedException("No autenticado con token OIDC");
    }

    public boolean isKeycloakToken() {
        return getAuthentication()
            .filter(auth -> auth instanceof JwtAuthenticationToken)
            .map(auth -> (JwtAuthenticationToken) auth)
            .map(JwtAuthenticationToken::getToken)
            .map(jwt -> {
                boolean hasKeycloakIssuer = jwt.getIssuer() != null && jwt.getIssuer().toString().contains("/realms/");
                boolean hasPreferredUsername = jwt.hasClaim("preferred_username");
                return hasKeycloakIssuer || hasPreferredUsername;
            })
            .orElse(false);
    }

    public Long getEscuelaId() {
        Long escuelaIdToken = getAuthentication()
            .filter(auth -> auth instanceof JwtAuthenticationToken)
            .map(auth -> ((JwtAuthenticationToken) auth).getToken().getClaim("escuela_id"))
            .map(this::toLong)
            .orElse(null);

        // Obtenemos el usuario de dominio (disparando auto-linking en el primer login)
        Usuario usuarioDominio = null;
        try {
            usuarioDominio = getUsuarioDominio();
        } catch (AccessDeniedException e) {
            if (escuelaIdToken != null) {
                return escuelaIdToken;
            }
            throw e;
        }

        if (escuelaIdToken != null) {
            return escuelaIdToken;
        }

        return Optional.ofNullable(usuarioDominio.getEscuela())
            .map(Escuela::getId)
            .orElseThrow(() -> new AccessDeniedException("Claim escuela_id ausente y sin escuela asignada"));
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

    /** Carga el registro de dominio vinculado al subject, o lo auto-vincula en su primer login. */
    public Usuario getUsuarioDominio() {
        return usuarioService.findOrLinkByKeycloakSubject(getSubject(), getPreferredUsername(), isKeycloakToken());
    }

    private Optional<Authentication> getAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}