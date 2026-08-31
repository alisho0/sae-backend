package com.rea.sae_backend.services;

import com.rea.sae_backend.dtos.EscuelaExcelImportResultDto;
import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.models.Role;
import com.rea.sae_backend.models.Usuario;
import com.rea.sae_backend.repositories.EscuelaRepository;
import com.rea.sae_backend.repositories.UsuarioRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscuelaExcelServiceTest {

    @Mock
    private EscuelaRepository escuelaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioService usuarioService;

    private EscuelaExcelService escuelaExcelService;

    @BeforeEach
    void setUp() {
        escuelaExcelService = new EscuelaExcelService(escuelaRepository, usuarioRepository, usuarioService);
    }

    /*
    @Test
    void cargarEscuelasDesdeExcel_CreaEscuelasYUsuariosDirectores() throws IOException {
        // Arrange
        when(escuelaRepository.findByCue(anyString())).thenReturn(Optional.empty());
        when(escuelaRepository.save(any(Escuela.class))).thenAnswer(invocation -> {
            Escuela e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(10L);
            return e;
        });

        when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(usuarioService.create(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(20L);
            return u;
        });

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Escuelas");

        // Header: nombre, cue, contraseña
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("nombre");
        header.createCell(1).setCellValue("cue");
        header.createCell(2).setCellValue("contraseña");

        // Fila 1: UNSE, 860228300, Unse7k
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("UNSE");
        row1.createCell(1).setCellValue(860228300);
        row1.createCell(2).setCellValue("Unse7k");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "escuelas.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                baos.toByteArray()
        );

        // Act
        EscuelaExcelImportResultDto resultado = escuelaExcelService.cargarEscuelasDesdeExcel(file);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalFilas());
        assertEquals(1, resultado.getExitosos());
        assertEquals(0, resultado.getFallidos());
        assertEquals(1, resultado.getEscuelasCargadas().size());
        assertEquals("UNSE", resultado.getEscuelasCargadas().get(0).getNombre());

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioService).create(usuarioCaptor.capture());

        Usuario usuarioCreado = usuarioCaptor.getValue();
        assertEquals("UNSE", usuarioCreado.getUsername());
        assertEquals("Unse7k", usuarioCreado.getPassword());
        assertEquals(Role.DIRECTOR, usuarioCreado.getRole());
    }
     */
}
