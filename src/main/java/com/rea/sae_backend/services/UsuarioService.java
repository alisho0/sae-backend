package com.rea.sae_backend.services;

import com.rea.sae_backend.models.Role;
import com.rea.sae_backend.models.Usuario;
import com.rea.sae_backend.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public Optional<Usuario> findByKeycloakSubject(String keycloakSubject) {
        return usuarioRepository.findByKeycloakSubject(keycloakSubject);
    }

    /** Vincula un registro de dominio al identity de Keycloak. */
    public void asignarSubject(String username, String subject) {
        findByUsername(username).ifPresent(u -> {
            u.setKeycloakSubject(subject);
            usuarioRepository.save(u);
        });
    }

    /**
     * Busca el usuario de dominio por su Keycloak Subject (UUID).
     * Si no se encuentra vinculado (ej. primer login con Keycloak tras migración),
     * busca por nombre de usuario de dominio y lo auto-vincula.
     *
     * @param keycloakSubject UUID del usuario emitido por Keycloak (claim 'sub').
     * @param preferredUsername Nombre de usuario proveniente del token OIDC (claim 'preferred_username').
     * @param isKeycloakToken Indica si el token proviene de Keycloak (para no auto-vincular tokens legacy).
     * @return El usuario de dominio persistido y vinculado.
     */
    @Transactional
    public Usuario findOrLinkByKeycloakSubject(String keycloakSubject, String preferredUsername, boolean isKeycloakToken) {
        if (keycloakSubject == null || keycloakSubject.isBlank()) {
            throw new AccessDeniedException("Token sin identificador de sujeto (sub)");
        }

        // 1. Caso ya vinculado: búsqueda rápida por subject
        Optional<Usuario> usuarioPorSubject = usuarioRepository.findByKeycloakSubject(keycloakSubject);
        if (usuarioPorSubject.isPresent()) {
            return usuarioPorSubject.get();
        }

        // 2. Si no es un token de Keycloak (ej. JWT legacy), solo buscamos por username sin modificar el subject
        String usernameToSearch = (preferredUsername != null && !preferredUsername.isBlank())
                ? preferredUsername
                : keycloakSubject;

        Optional<Usuario> usuarioPorUsername = usuarioRepository.findByUsername(usernameToSearch)
                .or(() -> usuarioRepository.findByUsernameIgnoreCase(usernameToSearch));

        if (usuarioPorUsername.isPresent()) {
            Usuario usuario = usuarioPorUsername.get();

            // Solo auto-vinculamos si es un token genuino de Keycloak y el usuario aún no tiene subject
            if (isKeycloakToken && (usuario.getKeycloakSubject() == null || usuario.getKeycloakSubject().isBlank())) {
                usuario.setKeycloakSubject(keycloakSubject);
                return usuarioRepository.save(usuario);
            }
            return usuario;
        }

        throw new AccessDeniedException("Usuario de dominio no encontrado para la identidad autenticada");
    }

    public Usuario create(Usuario usuario) {
        if (usuario.getRole() == null) {
            usuario.setRole(Role.DIRECTOR);
        }
        if (usuario.getPassword() != null && !usuario.getPassword().isBlank() && !isEncodedPassword(usuario.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    public Usuario update(Long id, Usuario usuarioDetails) {
        return usuarioRepository.findById(id)
            .map(existing -> {
                existing.setUsername(usuarioDetails.getUsername());
                if (usuarioDetails.getPassword() != null && !usuarioDetails.getPassword().isBlank() && !isEncodedPassword(usuarioDetails.getPassword())) {
                    existing.setPassword(passwordEncoder.encode(usuarioDetails.getPassword()));
                }
                existing.setEscuela(usuarioDetails.getEscuela());
                existing.setRole(usuarioDetails.getRole() != null ? usuarioDetails.getRole() : existing.getRole());
                if (usuarioDetails.getKeycloakSubject() != null) {
                    existing.setKeycloakSubject(usuarioDetails.getKeycloakSubject());
                }
                return usuarioRepository.save(existing);
            })
            .orElseGet(() -> {
                usuarioDetails.setId(id);
                if (usuarioDetails.getRole() == null) {
                    usuarioDetails.setRole(Role.DIRECTOR);
                }
                if (usuarioDetails.getPassword() != null && !usuarioDetails.getPassword().isBlank() && !isEncodedPassword(usuarioDetails.getPassword())) {
                    usuarioDetails.setPassword(passwordEncoder.encode(usuarioDetails.getPassword()));
                }
                return usuarioRepository.save(usuarioDetails);
            });
    }

    private boolean isEncodedPassword(String password) {
        return password != null && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    public void delete(Long id) {
        usuarioRepository.deleteById(id);
    }
}
