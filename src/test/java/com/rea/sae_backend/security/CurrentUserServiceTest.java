package com.rea.sae_backend.security;

import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.models.Usuario;
import com.rea.sae_backend.services.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Jwt createMockJwt(String subject, String preferredUsername, String issuer, Map<String, Object> extraClaims) {
        Jwt.Builder builder = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));

        if (preferredUsername != null) {
            builder.claim("preferred_username", preferredUsername);
        }
        if (issuer != null) {
            builder.issuer(issuer);
        }
        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }
        return builder.build();
    }

    @Test
    @DisplayName("Debe extraer el subject y username de Keycloak correctamente")
    void shouldExtractSubjectAndUsername() {
        Jwt jwt = createMockJwt("uuid-123", "director1", "http://localhost:8081/realms/sae_realm", null);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_DIRECTOR")), "uuid-123");

        when(securityContext.getAuthentication()).thenReturn(auth);

        assertThat(currentUserService.getSubject()).isEqualTo("uuid-123");
        assertThat(currentUserService.getPreferredUsername()).isEqualTo("director1");
        assertThat(currentUserService.isKeycloakToken()).isTrue();
    }

    @Test
    @DisplayName("Debe obtener escuelaId desde el claim del token si está presente")
    void shouldGetEscuelaIdFromClaim() {
        Jwt jwt = createMockJwt("uuid-123", "director1", "http://localhost:8081/realms/sae_realm", Map.of("escuela_id", 15L));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_DIRECTOR")), "uuid-123");

        when(securityContext.getAuthentication()).thenReturn(auth);

        Usuario mockUsuario = new Usuario();
        when(usuarioService.findOrLinkByKeycloakSubject("uuid-123", "director1", true)).thenReturn(mockUsuario);

        Long escuelaId = currentUserService.getEscuelaId();
        assertThat(escuelaId).isEqualTo(15L);
    }

    @Test
    @DisplayName("Debe hacer fallback a la escuela del usuario de dominio si el claim escuela_id está ausente")
    void shouldFallbackToDomainUserEscuelaIdWhenClaimIsMissing() {
        Jwt jwt = createMockJwt("uuid-123", "director1", "http://localhost:8081/realms/sae_realm", Map.of());
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_DIRECTOR")), "uuid-123");

        when(securityContext.getAuthentication()).thenReturn(auth);

        Escuela escuela = new Escuela();
        escuela.setId(42L);

        Usuario mockUsuario = new Usuario();
        mockUsuario.setEscuela(escuela);

        when(usuarioService.findOrLinkByKeycloakSubject("uuid-123", "director1", true)).thenReturn(mockUsuario);

        Long escuelaId = currentUserService.getEscuelaId();
        assertThat(escuelaId).isEqualTo(42L);
    }

    @Test
    @DisplayName("Debe reconocer rol de ADMIN correctamente")
    void shouldRecognizeAdminRole() {
        Jwt jwt = createMockJwt("uuid-admin", "admin", "http://localhost:8081/realms/sae_realm", null);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), "uuid-admin");

        when(securityContext.getAuthentication()).thenReturn(auth);

        assertThat(currentUserService.isAdmin()).isTrue();
        assertThat(currentUserService.getRoles()).contains("ADMIN");
    }
}
