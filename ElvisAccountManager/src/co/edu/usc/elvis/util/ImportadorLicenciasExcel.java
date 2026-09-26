package co.edu.usc.elvis.util;

import co.edu.usc.elvis.dao.LicenciaDAO;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Lee un archivo Excel (.xlsx) con las columnas Usuario | Contraseña | IP
 * y crea o actualiza las licencias correspondientes en la base de datos.
 * Fila 1 = encabezados, los datos empiezan en la fila 2.
 */
public class ImportadorLicenciasExcel {

    public static class ResultadoImportacion {
        public int agregadas = 0;
        public int actualizadas = 0;
        public int conError = 0;
    }

    private final LicenciaDAO licenciaDAO = new LicenciaDAO();
    private final DataFormatter formateador = new DataFormatter();

    public ResultadoImportacion importar(File archivo) throws IOException {
        ResultadoImportacion resultado = new ResultadoImportacion();

        try (FileInputStream fis = new FileInputStream(archivo);
             XSSFWorkbook libro = new XSSFWorkbook(fis)) {

            XSSFSheet hoja = libro.getSheetAt(0);

            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) {
                    continue;
                }

                String usuario = obtenerTexto(fila, 0);
                String contrasena = obtenerTexto(fila, 1);
                String ip = obtenerTexto(fila, 2);

                if (usuario.isEmpty()) {
                    continue;
                }

                try {
                    if (licenciaDAO.existeUsuario(usuario)) {
                        licenciaDAO.actualizarCredenciales(usuario, contrasena, ip);
                        resultado.actualizadas++;
                    } else {
                        licenciaDAO.insertarLicencia(usuario, contrasena, ip);
                        resultado.agregadas++;
                    }
                } catch (SQLException e) {
                    resultado.conError++;
                    System.err.println("Error al guardar licencia '" + usuario + "': " + e.getMessage());
                }
            }
        }

        return resultado;
    }

    private String obtenerTexto(Row fila, int indice) {
        Cell celda = fila.getCell(indice);
        if (celda == null) {
            return "";
        }
        return formateador.formatCellValue(celda).trim();
    }
}