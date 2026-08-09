package com.rea.sae_backend.services;

import com.rea.sae_backend.dtos.ExcelImportResultDto;
import com.rea.sae_backend.models.Alumno;
import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.repositories.AlumnoRepository;
import com.rea.sae_backend.repositories.EscuelaRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlumnoExcelServiceTest {

    @Mock
    private AlumnoRepository alumnoRepository;

    @Mock
    private EscuelaRepository escuelaRepository;

    private AlumnoExcelService alumnoExcelService;

    @BeforeEach
    void setUp() {
        alumnoExcelService = new AlumnoExcelService(alumnoRepository, escuelaRepository);
    }

    @Test
    void cargarAlumnosDesdeExcel_ProcesaCorrectamenteAlumnosYReportaEscuelasNoEncontradas() throws IOException {
        // Arrange
        Escuela escuelaExistente = new Escuela();
        escuelaExistente.setId(1L);
        escuelaExistente.setNombre("Escuela Informatica");
        escuelaExistente.setCue("860207900");

        when(escuelaRepository.findByCue("860207900")).thenReturn(Optional.of(escuelaExistente));
        when(escuelaRepository.findByCue("999999999")).thenReturn(Optional.empty());

        when(alumnoRepository.findByDni("16141384")).thenReturn(Optional.empty());
        when(alumnoRepository.save(any(Alumno.class))).thenAnswer(invocation -> {
            Alumno a = invocation.getArgument(0);
            a.setId(100L);
            return a;
        });

        // Crear Excel en memoria
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Alumnos");

        // Fila 0: Header
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("dni");
        header.createCell(1).setCellValue("apellido");
        header.createCell(2).setCellValue("nombre");
        header.createCell(3).setCellValue("nacimiento");
        header.createCell(4).setCellValue("localidad");
        header.createCell(5).setCellValue("curso");
        header.createCell(6).setCellValue("cue");
        header.createCell(7).setCellValue("cumple_asistencia");

        // Fila 1: Valida con CUE existente (16141384, SANCHEZ, MATIAS FERNANDO, 2002-02-09, EL BOBADAL, INFORMATICA, 860207900, False)
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue(16141384);
        row1.createCell(1).setCellValue("SANCHEZ");
        row1.createCell(2).setCellValue("MATIAS FERNANDO");
        row1.createCell(3).setCellValue("2002-02-09");
        row1.createCell(4).setCellValue("EL BOBADAL");
        row1.createCell(5).setCellValue("INFORMATICA");
        row1.createCell(6).setCellValue(860207900);
        row1.createCell(7).setCellValue("False");

        // Fila 2: CUE no existente (debe saltarse y reportarse)
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue(99999999);
        row2.createCell(1).setCellValue("PEREZ");
        row2.createCell(2).setCellValue("JUAN");
        row2.createCell(3).setCellValue("2005-05-05");
        row2.createCell(4).setCellValue("CAPITAL");
        row2.createCell(5).setCellValue("ELECTRONICA");
        row2.createCell(6).setCellValue(999999999);
        row2.createCell(7).setCellValue(true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "alumnos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                baos.toByteArray()
        );

        // Act
        ExcelImportResultDto resultado = alumnoExcelService.cargarAlumnosDesdeExcel(file);

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.getTotalFilas());
        assertEquals(1, resultado.getExitosos());
        assertEquals(1, resultado.getFallidos());
        assertEquals(1, resultado.getAlumnosCargados().size());
        assertEquals("16141384", resultado.getAlumnosCargados().get(0).getDni());
        assertEquals("SANCHEZ", resultado.getAlumnosCargados().get(0).getApellido());
        assertEquals("2002-02-09", resultado.getAlumnosCargados().get(0).getNacimiento());

        assertEquals(1, resultado.getErrores().size());
        assertTrue(resultado.getErrores().get(0).contains("Escuela con CUE '999999999' no fue encontrada"));

        verify(alumnoRepository, times(1)).save(any(Alumno.class));
    }
}
