package co.edu.usc.elvis.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenido de un reporte ya calculado (titulo, encabezado, resumen y tabla),
 * independiente del formato: la misma informacion se muestra en pantalla y
 * se exporta a PDF o Excel.
 */
public class ReporteDatos {

    public enum TipoColumna { TEXTO, ENTERO, PORCENTAJE }

    private final String codigo;
    private final String titulo;
    private String nombreHoja;
    private final List<String> lineasEncabezado = new ArrayList<>();
    private final List<String> resumen = new ArrayList<>();
    private final List<String> columnas = new ArrayList<>();
    private final List<TipoColumna> tipos = new ArrayList<>();
    private final List<Double> pesos = new ArrayList<>();
    private final List<List<Object>> filas = new ArrayList<>();

    public ReporteDatos(String codigo, String titulo) {
        this.codigo = codigo;
        this.titulo = titulo;
    }

    public void agregarLineaEncabezado(String linea) {
        lineasEncabezado.add(linea);
    }

    public void agregarResumen(String linea) {
        resumen.add(linea);
    }

    /** El peso es el ancho relativo de la columna en el PDF. */
    public void agregarColumna(String nombre, TipoColumna tipo, double peso) {
        columnas.add(nombre);
        tipos.add(tipo);
        pesos.add(peso);
    }

    public void agregarFila(Object... valores) {
        if (valores.length != columnas.size()) {
            throw new IllegalArgumentException("La fila tiene " + valores.length
                    + " valores y el reporte " + columnas.size() + " columnas.");
        }
        List<Object> fila = new ArrayList<>(valores.length);
        for (Object v : valores) {
            fila.add(v);
        }
        filas.add(fila);
    }

    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }

    /** Nombre corto de la hoja de Excel (maximo 31 caracteres); si no se define, se usa el titulo. */
    public String getNombreHoja() { return nombreHoja == null ? titulo : nombreHoja; }
    public void setNombreHoja(String nombreHoja) { this.nombreHoja = nombreHoja; }

    public List<String> getLineasEncabezado() { return lineasEncabezado; }
    public List<String> getResumen() { return resumen; }
    public List<String> getColumnas() { return columnas; }
    public List<TipoColumna> getTipos() { return tipos; }
    public List<Double> getPesos() { return pesos; }
    public List<List<Object>> getFilas() { return filas; }

    /** Texto con el que se muestra un valor en pantalla y en el PDF. */
    public static String formatear(Object valor, TipoColumna tipo) {
        if (valor == null) {
            return "";
        }
        if (tipo == TipoColumna.PORCENTAJE && valor instanceof Number) {
            return String.format("%.1f%%", ((Number) valor).doubleValue());
        }
        return String.valueOf(valor);
    }
}