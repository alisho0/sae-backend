package com.rea.sae_backend.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.rea.sae_backend.config.PeriodoConfig;
import com.rea.sae_backend.models.Alumno;
import com.rea.sae_backend.models.RegistroAsistencia;
import com.rea.sae_backend.repositories.RegistroAsistenciaRepository;
import com.rea.sae_backend.specifications.RegistroAsistenciaSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final RegistroAsistenciaRepository registroRepository;
    private final PeriodoConfig periodoConfig;

    public byte[] getAsistencia(Long escuelaId, String periodo) throws IOException {

        String p = periodoConfig.resolve(periodo);

        Specification<RegistroAsistencia> spec =
                Specification.where(
                                RegistroAsistenciaSpecification.periodoEquals(p)
                        )
                        .and(RegistroAsistenciaSpecification.escuelaIdEquals(escuelaId));

        List<RegistroAsistencia> registros = registroRepository.findAll(spec);

        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Asistencia");

            String[] headers = {
                    "nombre", "apellido", "dni", "fecha_de_nacimiento", "localidad",
                    "curso", "nombre_de_escuela", "cumple_asistencia",
                    "editado_por_escuela", "creado_por_escuela"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (RegistroAsistencia registro : registros) {
                Alumno alumno = registro.getAlumno();
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(alumno != null ? alumno.getNombre() : "");
                row.createCell(1).setCellValue(alumno != null ? alumno.getApellido() : "");
                row.createCell(2).setCellValue(alumno != null ? alumno.getDni() : "");
                row.createCell(3).setCellValue(alumno != null ? alumno.getNacimiento() : "");
                row.createCell(4).setCellValue(alumno != null ? alumno.getLocalidad() : "");
                row.createCell(5).setCellValue(registro.getCurso());
                row.createCell(6).setCellValue(
                        alumno != null && alumno.getEscuela() != null ? alumno.getEscuela().getNombre() : "");
                row.createCell(7).setCellValue(
                        Boolean.TRUE.equals(registro.getCumpleAsistencia()));
                row.createCell(8).setCellValue(
                        Boolean.TRUE.equals(registro.getEditadoPorEscuela()));
                row.createCell(9).setCellValue(
                        Boolean.TRUE.equals(registro.getCreadoPorEscuela()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }
}