package com.rea.sae_backend.services;

import com.rea.sae_backend.dtos.EscuelaExcelImportResultDto;
import com.rea.sae_backend.dtos.EscuelaResponseDto;
import com.rea.sae_backend.models.Escuela;
import com.rea.sae_backend.models.Role;
import com.rea.sae_backend.models.Usuario;
import com.rea.sae_backend.repositories.EscuelaRepository;
import com.rea.sae_backend.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EscuelaExcelService {

    private final EscuelaRepository escuelaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public EscuelaExcelImportResultDto cargarEscuelasDesdeExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo subido está vacío o es nulo");
        }

        List<EscuelaResponseDto> escuelasCargadas = new ArrayList<>();
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
                    String nombre = getCellValueAsString(row, columnIndexes.get("nombre"), dataFormatter);
                    String cue = getCellValueAsString(row, columnIndexes.get("cue"), dataFormatter);
                    String password = getCellValueAsString(row, columnIndexes.get("password"), dataFormatter);

                    if (nombre == null || nombre.isBlank()) {
                        fallidos++;
                        errores.add(String.format("Fila %d: El nombre de la escuela es obligatorio y está vacío", filaNumero));
                        continue;
                    }
                    if (cue == null || cue.isBlank()) {
                        fallidos++;
                        errores.add(String.format("Fila %d: El CUE es obligatorio y está vacío (Escuela: %s)", filaNumero, nombre));
                        continue;
                    }
                    if (password == null || password.isBlank()) {
                        fallidos++;
                        errores.add(String.format("Fila %d: La contraseña del usuario director es obligatoria y está vacía (Escuela: %s, CUE: %s)",
                                filaNumero, nombre, cue));
                        continue;
                    }

                    Escuela escuela = escuelaRepository.findByCue(cue).orElseGet(Escuela::new);
                    escuela.setNombre(nombre);
                    escuela.setCue(cue);
                    if (escuela.getAsistenciaCompletada() == null) {
                        escuela.setAsistenciaCompletada(false);
                    }
                    escuela = escuelaRepository.save(escuela);

                    Usuario usuario = escuela.getUsuario();
                    if (usuario == null) {
                        usuario = usuarioRepository.findByUsername(nombre).orElseGet(Usuario::new);
                    }

                    usuario.setUsername(cue);
                    usuario.setPassword(password);
                    usuario.setEscuela(escuela);
                    usuario.setRole(Role.DIRECTOR);

                    if (usuario.getId() == null) {
                        usuario = usuarioService.create(usuario);
                    } else {
                        usuario = usuarioService.update(usuario.getId(), usuario);
                    }

                    escuela.setUsuario(usuario);
                    escuela = escuelaRepository.save(escuela);

                    escuelasCargadas.add(EscuelaResponseDto.fromEntity(escuela));
                    exitosos++;

                } catch (Exception e) {
                    fallidos++;
                    errores.add(String.format("Fila %d: Error procesando escuela: %s", filaNumero, e.getMessage()));
                }
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error procesando el archivo Excel de escuelas: " + e.getMessage(), e);
        }

        return EscuelaExcelImportResultDto.builder()
                .totalFilas(totalFilas)
                .exitosos(exitosos)
                .fallidos(fallidos)
                .errores(errores)
                .escuelasCargadas(escuelasCargadas)
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
                        .replace("ú", "u")
                        .replace("ñ", "n");

                if (header.contains("nombre")) map.put("nombre", cell.getColumnIndex());
                else if (header.contains("cue")) map.put("cue", cell.getColumnIndex());
                else if (header.contains("contrasena") || header.contains("password") || header.contains("clave")) {
                    map.put("password", cell.getColumnIndex());
                }
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
