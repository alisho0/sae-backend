package com.rea.sae_backend.controllers;

import com.rea.sae_backend.dtos.UsuarioResponseDto;
import com.rea.sae_backend.models.Usuario;
import com.rea.sae_backend.services.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponseDto> list() {
        return usuarioService.findAll().stream()
                .map(UsuarioResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public UsuarioResponseDto getById(@PathVariable Long id) {
        return usuarioService.findById(id)
            .map(UsuarioResponseDto::fromEntity)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @PostMapping
    public UsuarioResponseDto create(@RequestBody Usuario usuario) {
        return UsuarioResponseDto.fromEntity(usuarioService.create(usuario));
    }

    @PutMapping("/{id}")
    public UsuarioResponseDto update(@PathVariable Long id, @RequestBody Usuario usuario) {
        return UsuarioResponseDto.fromEntity(usuarioService.update(id, usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        usuarioService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
