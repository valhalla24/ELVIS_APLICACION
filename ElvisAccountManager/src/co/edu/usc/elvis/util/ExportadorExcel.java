package co.edu.usc.elvis.util;

import co.edu.usc.elvis.model.ReporteDatos;
import co.edu.usc.elvis.model.ReporteDatos.TipoColumna;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Exporta un reporte a .xlsx con Apache POI (la misma libreria que ya se usa
 * para leer los Excel de licencias y listas). Los numeros y porcentajes se
 * guardan como numeros, para que se puedan ordenar y sumar en Excel.
 */
public class ExportadorExcel {

    private static final int ANCHO_MAXIMO = 60;
    private static final int ANCHO_MINIMO = 10;

    private ExportadorExcel() {
    }

    public static void exportar(ReporteDatos datos, File archivo) throws IOException {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            llenarLibro(libro, datos);
            try (FileOutputStream salida = new FileOutputStream(archivo)) {
                libro.write(salida);
            }
        }
    }

    /** Arma la hoja del reporte en cualquier libro de POI (la logica no depende del formato del archivo). */
    public static void llenarLibro(Workbook libro, ReporteDatos datos) {
        Sheet hoja = libro.createSheet(nombreHoja(datos.getNombreHoja()));

        Font fuenteTitulo = libro.createFont();
        fuenteTitulo.setBold(true);
        fuenteTitulo.setFontHeightInPoints((short) 14);
        Font fuenteNegrita = libro.createFont();
        fuenteNegrita.setBold(true);
        Font fuenteEncabezado = libro.createFont();
        fuenteEncabezado.setBold(true);
        fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());

        CellStyle estiloTitulo = libro.createCellStyle();
        estiloTitulo.setFont(fuenteTitulo);

        CellStyle estiloNegrita = libro.createCellStyle();
        estiloNegrita.setFont(fuenteNegrita);

        CellStyle estiloEncabezado = libro.createCellStyle();
        estiloEncabezado.setFont(fuenteEncabezado);
        estiloEncabezado.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estiloEncabezado.setAlignment(HorizontalAlignment.CENTER);
        estiloEncabezado.setVerticalAlignment(VerticalAlignment.CENTER);
        ponerBordes(estiloEncabezado);

        CellStyle estiloTexto = libro.createCellStyle();
        estiloTexto.setWrapText(true);
        estiloTexto.setVerticalAlignment(VerticalAlignment.TOP);
        ponerBordes(estiloTexto);

        CellStyle estiloEntero = libro.createCellStyle();
        estiloEntero.setAlignment(HorizontalAlignment.CENTER);
        estiloEntero.setVerticalAlignment(VerticalAlignment.TOP);
        estiloEntero.setDataFormat(libro.createDataFormat().getFormat("0"));
        ponerBordes(estiloEntero);

        CellStyle estiloPorcentaje = libro.createCellStyle();
        estiloPorcentaje.setAlignment(HorizontalAlignment.CENTER);
        estiloPorcentaje.setVerticalAlignment(VerticalAlignment.TOP);
        estiloPorcentaje.setDataFormat(libro.createDataFormat().getFormat("0.0\"%\""));
        ponerBordes(estiloPorcentaje);

        int fila = 0;
        escribirTexto(hoja, fila++, datos.getTitulo(), estiloTitulo);
        escribirTexto(hoja, fila++, "Universidad Santiago de Cali · DTSI · Servidor ELVIS", null);
        for (String linea : datos.getLineasEncabezado()) {
            escribirTexto(hoja, fila++, linea, null);
        }
        if (!datos.getResumen().isEmpty()) {
            fila++;
            escribirTexto(hoja, fila++, "Resumen", estiloNegrita);
            for (String linea : datos.getResumen()) {
                escribirTexto(hoja, fila++, "• " + linea, null);
            }
        }
        fila++;

        List<String> columnas = datos.getColumnas();
        List<TipoColumna> tipos = datos.getTipos();
        int filaEncabezado = fila;
        Row encabezado = hoja.createRow(fila++);
        for (int i = 0; i < columnas.size(); i++) {
            Cell celda = encabezado.createCell(i);
            celda.setCellValue(columnas.get(i));
            celda.setCellStyle(estiloEncabezado);
        }

        int[] anchos = new int[columnas.size()];
        for (int i = 0; i < columnas.size(); i++) {
            anchos[i] = columnas.get(i).length();
        }

        for (List<Object> valores : datos.getFilas()) {
            Row r = hoja.createRow(fila++);
            for (int i = 0; i < columnas.size(); i++) {
                Object v = valores.get(i);
                Cell celda = r.createCell(i);
                TipoColumna tipo = tipos.get(i);
                if (v instanceof Number && tipo != TipoColumna.TEXTO) {
                    celda.setCellValue(((Number) v).doubleValue());
                    celda.setCellStyle(tipo == TipoColumna.PORCENTAJE ? estiloPorcentaje : estiloEntero);
                } else {
                    celda.setCellValue(v == null ? "" : String.valueOf(v));
                    celda.setCellStyle(estiloTexto);
                }
                anchos[i] = Math.max(anchos[i], ReporteDatos.formatear(v, tipo).length());
            }
        }

        for (int i = 0; i < columnas.size(); i++) {
            int caracteres = Math.max(ANCHO_MINIMO, Math.min(ANCHO_MAXIMO, anchos[i] + 3));
            hoja.setColumnWidth(i, caracteres * 256);
        }

        hoja.createFreezePane(0, filaEncabezado + 1);
        if (!datos.getFilas().isEmpty()) {
            hoja.setAutoFilter(new CellRangeAddress(filaEncabezado, fila - 1, 0, columnas.size() - 1));
        }
    }

    private static void escribirTexto(Sheet hoja, int indiceFila, String texto, CellStyle estilo) {
        Row r = hoja.createRow(indiceFila);
        Cell celda = r.createCell(0);
        celda.setCellValue(texto);
        if (estilo != null) {
            celda.setCellStyle(estilo);
        }
    }

    private static void ponerBordes(CellStyle estilo) {
        estilo.setBorderTop(BorderStyle.THIN);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBorderLeft(BorderStyle.THIN);
        estilo.setBorderRight(BorderStyle.THIN);
    }

    /** Excel no acepta ciertos caracteres en el nombre de la hoja ni mas de 31. */
    static String nombreHoja(String titulo) {
        String limpio = titulo.replaceAll("[\\\\/*?:\\[\\]]", " ").trim();
        if (limpio.isEmpty()) {
            return "Reporte";
        }
        return limpio.length() > 31 ? limpio.substring(0, 31).trim() : limpio;
    }
}