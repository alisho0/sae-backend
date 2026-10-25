package com.rea.sae_backend.services;

import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.models.Role;
import com.rea.sae_backend.models.Usuario;
import com.rea.sae_backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setUsername("cue_0600123");
        usuarioMock.setRole(Role.DIRECTOR);

        Escuela escuela = new Escuela();
        escuela.setId(10L);
        escuela.setNombre("Escuela Primaria 1");
        usuarioMock.setEscuela(escuela);
    }

    @Test
    @DisplayName("Debe retornar el usuario directamente cuando ya está vinculado por keycloakSubject")
    void shouldReturnUserWhenAlreadyLinkedBySubject() {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        usuarioMock.setKeycloakSubject(uuid);

        when(usuarioRepository.findByKeycloakSubject(uuid)).thenReturn(Optional.of(usuarioMock));

        Usuario result = usuarioService.findOrLinkByKeycloakSubject(uuid, "cue_0600123", true);

        assertThat(result).isNotNull();
        assertThat(result.getKeycloakSubject()).isEqualTo(uuid);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe autovincular el usuario cuando viene de Keycloak y coincide su username")
    void shouldAutoLinkUserWhenFoundByUsernameWithKeycloakToken() {
        String uuid = "a4e82f12-78d1-4122-b9b0-97ef938c4d1e";
        usuarioMock.setKeycloakSubject(null);

        when(usuarioRepository.findByKeycloakSubject(uuid)).thenReturn(Optional.empty());
        when(usuarioRepository.findByUsername("cue_0600123")).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario result = usuarioService.findOrLinkByKeycloakSubject(uuid, "cue_0600123", true);

        assertThat(result).isNotNull();
        assertThat(result.getKeycloakSubject()).isEqualTo(uuid);
        verify(usuarioRepository, times(1)).save(usuarioMock);
    }

    @Test
    @DisplayName("No debe autovincular el keycloakSubject si el token es legacy")
    void shouldNotAutoLinkWhenTokenIsLegacy() {
        String username = "cue_0600123";
        usuarioMock.setKeycloakSubject(null);

        when(usuarioRepository.findByKeycloakSubject(username)).thenReturn(Optional.empty());
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuarioMock));

        Usuario result = usuarioService.findOrLinkByKeycloakSubject(username, username, false);

        assertThat(result).isNotNull();
        assertThat(result.getKeycloakSubject()).isNull();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar AccessDeniedException cuando el usuario no existe en la BD de dominio")
    void shouldThrowAccessDeniedExceptionWhenUserNotFound() {
        String uuid = "unknown-uuid";
        when(usuarioRepository.findByKeycloakSubject(uuid)).thenReturn(Optional.empty());
        when(usuarioRepository.findByUsername("no_existe")).thenReturn(Optional.empty());
        when(usuarioRepository.findByUsernameIgnoreCase("no_existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.findOrLinkByKeycloakSubject(uuid, "no_existe", true))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Usuario de dominio no encontrado");
    }

    @Test
    @DisplayName("Debe lanzar AccessDeniedException si el subject es nulo o vacío")
    void shouldThrowAccessDeniedExceptionWhenSubjectIsBlank() {
        assertThatThrownBy(() -> usuarioService.findOrLinkByKeycloakSubject("", "cue_0600123", true))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Token sin identificador de sujeto (sub)");
    }
}
