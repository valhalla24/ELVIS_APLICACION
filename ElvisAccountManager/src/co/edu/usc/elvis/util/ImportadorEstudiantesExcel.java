package co.edu.usc.elvis.util;

import co.edu.usc.elvis.model.Estudiante;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Lee un archivo Excel (.xlsx) con una lista de estudiantes. En vez de asumir
 * una posición fija de columnas (A, B, C...), busca en la fila 1 los
 * encabezados por palabras clave (Código, Nombre / Nombre y Apellidos, o
 * Nombre + Apellidos por separado, Correo), sin importar el orden en que
 * vengan ni cómo esté armado el archivo de cada profesor.
 * Solo hace la LECTURA/parseo; guardar en la base de datos es un paso aparte
 * (para poder mostrar una vista previa antes de confirmar).
 */
public class ImportadorEstudiantesExcel {

    private final DataFormatter formateador = new DataFormatter();

    public List<Estudiante> leer(File archivo) throws IOException {
        List<Estudiante> estudiantes = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(archivo);
             XSSFWorkbook libro = new XSSFWorkbook(fis)) {

            XSSFSheet hoja = libro.getSheetAt(0);
            Row filaEncabezados = hoja.getRow(0);
            if (filaEncabezados == null) {
                throw new IOException("El archivo está vacío o no tiene fila de encabezados.");
            }

            Map<String, Integer> columnas = mapearEncabezados(filaEncabezados);

            Integer colCodigo = buscarColumna(columnas, "codigo", "cod", "id");
            Integer colCorreo = buscarColumna(columnas, "correo", "email", "mail");
            Integer colNombreCompleto = buscarColumna(columnas,
                    "nombrecompleto", "nombreyapellidos", "estudiante", "nombre");
            Integer colApellido = buscarColumna(columnas, "apellidos", "apellido");

            if (colCodigo == null) {
                throw new IOException("No encontré una columna de 'Código' en los encabezados del archivo.");
            }
            if (colNombreCompleto == null) {
                throw new IOException("No encontré una columna de 'Nombre' en los encabezados del archivo.");
            }

            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) {
                    continue;
                }

                String codigo = obtenerTexto(fila, colCodigo);
                String correo = obtenerTexto(fila, colCorreo);
                String nombre = obtenerTexto(fila, colNombreCompleto);
                String apellido = obtenerTexto(fila, colApellido);
                String nombreCompleto = apellido.isEmpty() ? nombre : (nombre + " " + apellido).trim();

                if (codigo.isEmpty() && nombreCompleto.isEmpty()) {
                    continue;
                }

                estudiantes.add(new Estudiante(nombreCompleto, codigo, correo, 0));
            }
        }

        return estudiantes;
    }

    private Map<String, Integer> mapearEncabezados(Row filaEncabezados) {
        Map<String, Integer> columnas = new HashMap<>();
        for (Cell celda : filaEncabezados) {
            String texto = normalizar(formateador.formatCellValue(celda));
            if (!texto.isEmpty()) {
                columnas.put(texto, celda.getColumnIndex());
            }
        }
        return columnas;
    }

    private Integer buscarColumna(Map<String, Integer> columnas, String... posiblesNombres) {
        for (String nombre : posiblesNombres) {
            for (Map.Entry<String, Integer> entrada : columnas.entrySet()) {
                if (entrada.getKey().contains(nombre)) {
                    return entrada.getValue();
                }
            }
        }
        return null;
    }

    private String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String obtenerTexto(Row fila, Integer indice) {
        if (indice == null) {
            return "";
        }
        Cell celda = fila.getCell(indice);
        if (celda == null) {
            return "";
        }
        return formateador.formatCellValue(celda).trim();
    }
}