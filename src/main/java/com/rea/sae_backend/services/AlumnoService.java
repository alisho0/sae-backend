package com.rea.sae_backend.services;

import com.rea.sae_backend.dtos.AlumnoRequestDto;
import com.rea.sae_backend.dtos.AlumnoResponseDto;
import com.rea.sae_backend.dtos.AsistenciaRequestDto;
import com.rea.sae_backend.dtos.ExcelImportResultDto;
import com.rea.sae_backend.models.Alumno;
import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.repositories.AlumnoRepository;
import com.rea.sae_backend.repositories.EscuelaRepository;
import com.rea.sae_backend.specifications.AlumnoSpecification;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlumnoService {

    private final AlumnoRepository alumnoRepository;
    private final EscuelaRepository escuelaRepository;

    public Page<Alumno> findAll(Pageable pageable, Boolean cumpleAsistencia, String dni, Long escuelaId) {
        Specification<Alumno> spec = Specification
                .where(AlumnoSpecification.dniEquals(dni))
                .and(AlumnoSpecification.cumpleAsistencia(cumpleAsistencia))
                .and(AlumnoSpecification.escuelaIdEquals(escuelaId));
        return alumnoRepository.findAll(spec, pageable);
    }

    public Optional<Alumno> findById(Long id) {
        return alumnoRepository.findById(id);
    }

    public List<Alumno> findAllByEscuela(Long escuelaId) {
        return alumnoRepository.findByEscuelaId(escuelaId);
    }

    public Alumno create(AlumnoRequestDto alumno) {
        if (alumno.getCumpleAsistencia() == null) {
            alumno.setCumpleAsistencia(false);
        }
        if (alumno.getCreadoPorEscuela() == null) {
            alumno.setCreadoPorEscuela(false);
        }
        Alumno alumnoModel = new Alumno();
        alumnoModel.setNombre(alumno.getNombre());
        alumnoModel.setApellido(alumno.getApellido());
        alumnoModel.setCurso(alumno.getCurso());
        alumnoModel.setDni(alumno.getDni());
        alumnoModel.setLocalidad(alumno.getLocalidad());
        alumnoModel.setNacimiento(alumno.getNacimiento());
        alumnoModel.setCumpleAsistencia(alumno.getCumpleAsistencia());
        alumnoModel.setCreadoPorEscuela(alumno.getCreadoPorEscuela());

        Escuela escuela = escuelaRepository.findById(alumno.getEscuelaId())
            .orElseThrow(() -> new RuntimeException("Escuela no encontrada"));
        alumnoModel.setEscuela(escuela);

        return alumnoRepository.save(alumnoModel);
    }

    public Boolean updateAsistencia(Long id, boolean cumpleAsistencia) {
        Alumno a = alumnoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        
        a.setCumpleAsistencia(cumpleAsistencia);
        alumnoRepository.save(a);
        return a.getCumpleAsistencia();
    }

    public Alumno update(Long id, AlumnoRequestDto dto) {

    return alumnoRepository.findById(id)
        .map(existing -> {

            existing.setNombre(dto.getNombre());
            existing.setApellido(dto.getApellido());
            existing.setCurso(dto.getCurso());
            existing.setDni(dto.getDni());
            existing.setLocalidad(dto.getLocalidad());
            existing.setNacimiento(dto.getNacimiento());

            if (dto.getEscuelaId() != null) {
                Escuela escuela = escuelaRepository.findById(dto.getEscuelaId())
                    .orElseThrow(() ->
                        new RuntimeException("Escuela no encontrada")
                    );
                existing.setEscuela(escuela);
            }
            existing.setCumpleAsistencia(dto.getCumpleAsistencia() != null ? dto.getCumpleAsistencia() : existing.getCumpleAsistencia());
            existing.setCreadoPorEscuela(dto.getCreadoPorEscuela() != null ? dto.getCreadoPorEscuela() : existing.getCreadoPorEscuela());
            existing.setEditadoPorEscuela(dto.getEditadoPorEscuela() != null ? dto.getEditadoPorEscuela() : existing.getEditadoPorEscuela());

            return alumnoRepository.save(existing);
        })
        .orElseThrow(() ->
            new RuntimeException("Alumno no encontrado")
        );
    }

    public void delete(Long id) {
        alumnoRepository.deleteById(id);
    }

    

    @Transactional
    public ExcelImportResultDto cargarAlumnosDesdeExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo subido está vacío o es nulo");
        }

        List<AlumnoResponseDto> alumnosCargados = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        int totalFilas = 0;
        int exitosos = 0;
        int fallidos = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hoja de cálculo está vacía");
            }

            DataFormatter dataFormatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se encontró fila de encabezado en el Excel");
            }

            Map<String, Integer> columnIndexes = buildColumnMap(headerRow, dataFormatter);

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                totalFilas++;
                int filaNumero = r + 1;

                try {
                    String dni = getCellValueAsString(row, columnIndexes.get("dni"), dataFormatter);
                    String apellido = getCellValueAsString(row, columnIndexes.get("apellido"), dataFormatter);
                    String nombre = getCellValueAsString(row, columnIndexes.get("nombre"), dataFormatter);
                    String nacimiento = getCellValueAsString(row, columnIndexes.get("nacimiento"), dataFormatter);
                    String localidad = getCellValueAsString(row, columnIndexes.get("localidad"), dataFormatter);
                    String curso = getCellValueAsString(row, columnIndexes.get("curso"), dataFormatter);
                    String cue = getCellValueAsString(row, columnIndexes.get("cue"), dataFormatter);
                    Boolean cumpleAsistencia = getCellValueAsBoolean(row, columnIndexes.get("cumple_asistencia"), dataFormatter);

                    if (cue == null || cue.isBlank()) {
                        fallidos++;
                        errores.add(String.format("Fila %d: El CUE es obligatorio y está vacío (DNI: %s, Alumno: %s %s)",
                                filaNumero, dni, apellido, nombre));
                        continue;
                    }

                    Optional<Escuela> escuelaOptional = escuelaRepository.findByCue(cue);
                    if (escuelaOptional.isEmpty()) {
                        fallidos++;
                        errores.add(String.format("Fila %d: Escuela con CUE '%s' no fue encontrada en la base de datos. Alumno omitido (DNI: %s, Alumno: %s %s)",
                                filaNumero, cue, dni, apellido, nombre));
                        continue;
                    }

                    Escuela escuela = escuelaOptional.get();

                    Alumno alumno;
                    if (dni != null && !dni.isBlank()) {
                        alumno = alumnoRepository.findByDni(dni).orElseGet(Alumno::new);
                    } else {
                        alumno = new Alumno();
                    }

                    alumno.setDni(dni);
                    alumno.setApellido(apellido);
                    alumno.setNombre(nombre);
                    alumno.setNacimiento(nacimiento);
                    alumno.setLocalidad(localidad);
                    alumno.setCurso(curso);
                    alumno.setCumpleAsistencia(cumpleAsistencia != null ? cumpleAsistencia : false);
                    alumno.setCreadoPorEscuela(false);
                    alumno.setEscuela(escuela);

                    Alumno guardado = alumnoRepository.save(alumno);
                    alumnosCargados.add(AlumnoResponseDto.fromEntity(guardado));
                    exitosos++;

                } catch (Exception e) {
                    fallidos++;
                    errores.add(String.format("Fila %d: Error procesando datos: %s", filaNumero, e.getMessage()));
                }
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error procesando el archivo Excel: " + e.getMessage(), e);
        }

        return ExcelImportResultDto.builder()
                .totalFilas(totalFilas)
                .exitosos(exitosos)
                .fallidos(fallidos)
                .errores(errores)
                .alumnosCargados(alumnosCargados)
                .build();
    }

    private Map<String, Integer> buildColumnMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell != null) {
                String header = formatter.formatCellValue(cell).trim().toLowerCase()
                        .replace(" ", "_")
                        .replace("á", "a")
                        .replace("é", "e")
                        .replace("í", "i")
                        .replace("ó", "o")
                        .replace("ú", "u");

                if (header.contains("dni")) map.put("dni", cell.getColumnIndex());
                else if (header.contains("apellido")) map.put("apellido", cell.getColumnIndex());
                else if (header.contains("nombre")) map.put("nombre", cell.getColumnIndex());
                else if (header.contains("nacimiento") || header.contains("fecha")) map.put("nacimiento", cell.getColumnIndex());
                else if (header.contains("localidad")) map.put("localidad", cell.getColumnIndex());
                else if (header.contains("curso")) map.put("curso", cell.getColumnIndex());
                else if (header.contains("cue")) map.put("cue", cell.getColumnIndex());
                else if (header.contains("cumple") || header.contains("asistencia")) map.put("cumple_asistencia", cell.getColumnIndex());
            }
        }
        return map;
    }

    private String getCellValueAsString(Row row, Integer columnIndex, DataFormatter formatter) {
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        return formatter.formatCellValue(cell).trim();
    }

    private Boolean getCellValueAsBoolean(Row row, Integer columnIndex, DataFormatter formatter) {
        if (columnIndex == null) {
            return false;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return false;
        }
        if (cell.getCellType() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        }
        String text = formatter.formatCellValue(cell).trim().toLowerCase();
        return "true".equals(text) || "1".equals(text) || "si".equals(text) || "s".equals(text) || "verdadero".equals(text);
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}
