package com.rea.sae_backend.services;

import com.rea.sae_backend.models.Role;
import com.rea.sae_backend.models.Usuario;
import com.rea.sae_backend.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public Usuario create(Usuario usuario) {
        if (usuario.getRole() == null) {
            usuario.setRole(Role.DIRECTOR);
        }
        if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    public Usuario update(Long id, Usuario usuarioDetails) {
        return usuarioRepository.findById(id)
            .map(existing -> {
                existing.setUsername(usuarioDetails.getUsername());
                if (usuarioDetails.getPassword() != null && !usuarioDetails.getPassword().isBlank()) {
                    existing.setPassword(passwordEncoder.encode(usuarioDetails.getPassword()));
                }
                existing.setEscuela(usuarioDetails.getEscuela());
                existing.setRole(usuarioDetails.getRole() != null ? usuarioDetails.getRole() : existing.getRole());
                return usuarioRepository.save(existing);
            })
            .orElseGet(() -> {
                usuarioDetails.setId(id);
                if (usuarioDetails.getRole() == null) {
                    usuarioDetails.setRole(Role.DIRECTOR);
                }
                if (usuarioDetails.getPassword() != null && !usuarioDetails.getPassword().isBlank()) {
                    usuarioDetails.setPassword(passwordEncoder.encode(usuarioDetails.getPassword()));
                }
                return usuarioRepository.save(usuarioDetails);
            });
    }

    public void delete(Long id) {
        usuarioRepository.deleteById(id);
    }
}
