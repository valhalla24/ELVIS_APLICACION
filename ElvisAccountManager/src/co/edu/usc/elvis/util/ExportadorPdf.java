package co.edu.usc.elvis.util;

import co.edu.usc.elvis.model.ReporteDatos;
import co.edu.usc.elvis.model.ReporteDatos.TipoColumna;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Genera un PDF (A4 horizontal) con un reporte, SIN librerias externas:
 * escribe directamente el formato PDF 1.4 usando la fuente Helvetica, que
 * todo lector de PDF trae incorporada. Pagina solo la tabla, repite los
 * encabezados en cada hoja y numera "Pagina X de N".
 */
public class ExportadorPdf {

    private static final double ANCHO_PAGINA = 842;
    private static final double ALTO_PAGINA = 595;
    private static final double MARGEN = 40;
    private static final double MARGEN_INFERIOR = 46;
    private static final double ALTO_LINEA = 10.5;
    private static final double TAM_CELDA = 8;
    private static final double TAM_ENCABEZADO = 8.5;
    private static final double PADDING_HORIZONTAL = 4;
    private static final int MAX_LINEAS_POR_CELDA = 6;

    private static final Charset CP1252 = Charset.forName("windows-1252");

    private static final double[] NEGRO = {0.10, 0.12, 0.16};
    private static final double[] GRIS = {0.40, 0.44, 0.50};
    private static final double[] AZUL = {0.086, 0.137, 0.247};
    private static final double[] BLANCO = {1, 1, 1};

    // Anchos de los caracteres ASCII 32..126 de Helvetica y Helvetica-Bold (milesimas de punto).
    private static final int[] ANCHO_NORMAL = {
        278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
        1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
        333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
        556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584};
    private static final int[] ANCHO_NEGRITA = {
        278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
        556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
        975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
        667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
        333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
        611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584};

    private final ReporteDatos datos;
    private final List<StringBuilder> paginas = new ArrayList<>();
    private StringBuilder pagina;
    private double y;
    private double[] anchosColumna;
    private double anchoTabla;

    private ExportadorPdf(ReporteDatos datos) {
        this.datos = datos;
    }

    public static void exportar(ReporteDatos datos, File archivo) throws IOException {
        byte[] pdf = generar(datos);
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            salida.write(pdf);
        }
    }

    public static byte[] generar(ReporteDatos datos) {
        return new ExportadorPdf(datos).construir();
    }

    // ------------------------------------------------------------------ maquetacion

    private byte[] construir() {
        calcularAnchos();
        nuevaPagina(true);
        dibujarEncabezadoTabla();

        if (datos.getFilas().isEmpty()) {
            texto(MARGEN + 4, y - 14, "No hay datos para este reporte.", 9, false, GRIS);
            y -= 24;
        }
        int indice = 0;
        for (List<Object> fila : datos.getFilas()) {
            dibujarFila(fila, indice++);
        }
        return ensamblar();
    }

    private void calcularAnchos() {
        anchoTabla = ANCHO_PAGINA - 2 * MARGEN;
        List<Double> pesos = datos.getPesos();
        double suma = 0;
        for (double p : pesos) {
            suma += p;
        }
        anchosColumna = new double[pesos.size()];
        for (int i = 0; i < anchosColumna.length; i++) {
            anchosColumna[i] = anchoTabla * pesos.get(i) / suma;
        }
    }

    private void nuevaPagina(boolean primera) {
        pagina = new StringBuilder();
        paginas.add(pagina);
        y = ALTO_PAGINA - MARGEN;
        if (primera) {
            dibujarCabeceraInforme();
        } else {
            texto(MARGEN, y - 9, datos.getTitulo() + " (continuación)", 9, true, GRIS);
            y -= 22;
        }
    }

    private void dibujarCabeceraInforme() {
        texto(MARGEN, y - 16, datos.getTitulo(), 16, true, AZUL);
        y -= 24;
        texto(MARGEN, y - 9, "Universidad Santiago de Cali · DTSI · Servidor ELVIS", 8.5, false, GRIS);
        y -= 15;
        for (String linea : datos.getLineasEncabezado()) {
            texto(MARGEN, y - 9, linea, 9, false, NEGRO);
            y -= 13;
        }
        y -= 2;
        linea(MARGEN, y, ANCHO_PAGINA - MARGEN, y, 0.8, 0.8, 0.8, 0.6);
        y -= 10;

        if (!datos.getResumen().isEmpty()) {
            double alto = datos.getResumen().size() * 13 + 10;
            rectangulo(MARGEN, y - alto, ANCHO_PAGINA - 2 * MARGEN, alto, 0.93, 0.95, 0.98);
            double yy = y - 8;
            for (String linea : datos.getResumen()) {
                texto(MARGEN + 8, yy - 9, "• " + linea, 9, false, NEGRO);
                yy -= 13;
            }
            y -= alto + 12;
        }
    }

    private void dibujarEncabezadoTabla() {
        List<String> columnas = datos.getColumnas();
        List<TipoColumna> tipos = datos.getTipos();

        List<List<String>> lineasPorColumna = new ArrayList<>();
        int maxLineas = 1;
        for (int i = 0; i < columnas.size(); i++) {
            List<String> lineas = envolverEncabezado(columnas.get(i), anchosColumna[i] - 2 * PADDING_HORIZONTAL);
            lineasPorColumna.add(lineas);
            maxLineas = Math.max(maxLineas, lineas.size());
        }

        double alto = Math.max(18, maxLineas * ALTO_LINEA + 7);
        rectangulo(MARGEN, y - alto, anchoTabla, alto, AZUL[0], AZUL[1], AZUL[2]);
        double x = MARGEN;
        for (int i = 0; i < columnas.size(); i++) {
            boolean centrado = tipos.get(i) != TipoColumna.TEXTO;
            List<String> lineas = lineasPorColumna.get(i);
            for (int k = 0; k < lineas.size(); k++) {
                String l = lineas.get(k);
                double tx = centrado
                        ? x + (anchosColumna[i] - ancho(l, true, TAM_ENCABEZADO)) / 2
                        : x + PADDING_HORIZONTAL;
                texto(tx, y - 12 - k * ALTO_LINEA, l, TAM_ENCABEZADO, true, BLANCO);
            }
            x += anchosColumna[i];
        }
        y -= alto;
    }

    /** Parte un encabezado en hasta 3 lineas por espacios (nunca a mitad de palabra). */
    private List<String> envolverEncabezado(String texto, double anchoMaximo) {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.trim().split("\\s+")) {
            String candidato = actual.length() == 0 ? palabra : actual + " " + palabra;
            if (actual.length() > 0 && ancho(candidato, true, TAM_ENCABEZADO) > anchoMaximo) {
                lineas.add(recortar(actual.toString(), anchoMaximo, true, TAM_ENCABEZADO));
                actual.setLength(0);
                actual.append(palabra);
            } else {
                actual.setLength(0);
                actual.append(candidato);
            }
        }
        if (actual.length() > 0 || lineas.isEmpty()) {
            lineas.add(recortar(actual.toString(), anchoMaximo, true, TAM_ENCABEZADO));
        }
        return lineas.size() > 3 ? new ArrayList<>(lineas.subList(0, 3)) : lineas;
    }

    private void dibujarFila(List<Object> fila, int indice) {
        List<TipoColumna> tipos = datos.getTipos();
        List<List<String>> celdas = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        int maxLineas = 1;
        boolean resaltada = false;
        for (int i = 0; i < anchosColumna.length; i++) {
            String t = ReporteDatos.formatear(fila.get(i), tipos.get(i));
            textos.add(t);
            if ("Alta".equals(t)) {
                resaltada = true;
            }
            List<String> lineas = envolver(t, anchosColumna[i] - 2 * PADDING_HORIZONTAL, TAM_CELDA);
            celdas.add(lineas);
            maxLineas = Math.max(maxLineas, lineas.size());
        }

        double alto = maxLineas * ALTO_LINEA + 6;
        if (y - alto < MARGEN_INFERIOR) {
            nuevaPagina(false);
            dibujarEncabezadoTabla();
        }

        if (resaltada) {
            rectangulo(MARGEN, y - alto, anchoTabla, alto, 0.99, 0.89, 0.88);
        } else if (indice % 2 == 1) {
            rectangulo(MARGEN, y - alto, anchoTabla, alto, 0.96, 0.97, 0.99);
        }

        double x = MARGEN;
        for (int i = 0; i < anchosColumna.length; i++) {
            boolean centrado = tipos.get(i) != TipoColumna.TEXTO;
            List<String> lineas = celdas.get(i);
            for (int k = 0; k < lineas.size(); k++) {
                String l = lineas.get(k);
                double tx = centrado
                        ? x + (anchosColumna[i] - ancho(l, false, TAM_CELDA)) / 2
                        : x + PADDING_HORIZONTAL;
                texto(tx, y - 3 - TAM_CELDA - k * ALTO_LINEA, l, TAM_CELDA, false, NEGRO);
            }
            x += anchosColumna[i];
        }
        linea(MARGEN, y - alto, MARGEN + anchoTabla, y - alto, 0.85, 0.86, 0.89, 0.4);
        y -= alto;
    }

    // ------------------------------------------------------------------ texto

    /** Parte el texto en lineas que quepan en el ancho dado (como mucho MAX_LINEAS_POR_CELDA). */
    private List<String> envolver(String texto, double anchoMaximo, double tam) {
        List<String> lineas = new ArrayList<>();
        String limpio = texto == null ? "" : texto.trim();
        StringBuilder actual = new StringBuilder();

        for (String palabra : limpio.split("\\s+")) {
            String p = palabra;
            while (!p.isEmpty() && ancho(p, false, tam) > anchoMaximo) {
                int n = 1;
                while (n < p.length() && ancho(p.substring(0, n + 1), false, tam) <= anchoMaximo) {
                    n++;
                }
                if (actual.length() > 0) {
                    lineas.add(actual.toString());
                    actual.setLength(0);
                }
                lineas.add(p.substring(0, n));
                p = p.substring(n);
            }
            if (p.isEmpty()) {
                continue;
            }
            String candidato = actual.length() == 0 ? p : actual + " " + p;
            if (ancho(candidato, false, tam) <= anchoMaximo) {
                actual.setLength(0);
                actual.append(candidato);
            } else {
                lineas.add(actual.toString());
                actual.setLength(0);
                actual.append(p);
            }
        }
        if (actual.length() > 0 || lineas.isEmpty()) {
            lineas.add(actual.toString());
        }

        if (lineas.size() > MAX_LINEAS_POR_CELDA) {
            List<String> recortadas = new ArrayList<>(lineas.subList(0, MAX_LINEAS_POR_CELDA));
            String ultima = recortadas.get(MAX_LINEAS_POR_CELDA - 1);
            recortadas.set(MAX_LINEAS_POR_CELDA - 1, recortar(ultima + "…", anchoMaximo, false, tam));
            return recortadas;
        }
        return lineas;
    }

    /** Corta el texto con "…" si no cabe en una sola linea. */
    private String recortar(String texto, double anchoMaximo, boolean negrita, double tam) {
        if (ancho(texto, negrita, tam) <= anchoMaximo) {
            return texto;
        }
        String base = texto.endsWith("…") ? texto.substring(0, texto.length() - 1) : texto;
        while (!base.isEmpty() && ancho(base + "…", negrita, tam) > anchoMaximo) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "…";
    }

    static double ancho(String texto, boolean negrita, double tam) {
        int[] tabla = negrita ? ANCHO_NEGRITA : ANCHO_NORMAL;
        double total = 0;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            int w;
            if (c >= 32 && c <= 126) {
                w = tabla[c - 32];
            } else if (c == '•') {
                w = 350;
            } else if (c == '—') {
                w = 1000;
            } else if (c == '…') {
                w = 1000;
            } else if (c == '·') {
                w = 278;
            } else if (c == '¿') {
                w = 611;
            } else if (c == '¡') {
                w = 333;
            } else {
                // letras con tilde: mismo ancho que la letra base
                String base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD);
                char b = base.isEmpty() ? '?' : base.charAt(0);
                w = (b >= 32 && b <= 126) ? tabla[b - 32] : 556;
            }
            total += w;
        }
        return total * tam / 1000.0;
    }

    // ------------------------------------------------------------------ operadores PDF

    private void texto(double x, double yy, String contenido, double tam, boolean negrita, double[] color) {
        pagina.append(String.format(Locale.US,
                "BT /%s %.2f Tf %.3f %.3f %.3f rg 1 0 0 1 %.2f %.2f Tm (%s) Tj ET\n",
                negrita ? "F2" : "F1", tam, color[0], color[1], color[2], x, yy, escapar(contenido)));
    }

    private void rectangulo(double x, double yy, double w, double h, double r, double g, double b) {
        pagina.append(String.format(Locale.US, "%.3f %.3f %.3f rg %.2f %.2f %.2f %.2f re f\n", r, g, b, x, yy, w, h));
    }

    private void linea(double x1, double y1, double x2, double y2, double r, double g, double b, double grosor) {
        pagina.append(String.format(Locale.US, "%.3f %.3f %.3f RG %.2f w %.2f %.2f m %.2f %.2f l S\n",
                r, g, b, grosor, x1, y1, x2, y2));
    }

    /** Convierte el texto a una cadena PDF solo-ASCII (los acentos van como codigos octales de Windows-1252). */
    static String escapar(String texto) {
        StringBuilder sb = new StringBuilder();
        for (byte bt : texto.getBytes(CP1252)) {
            int b = bt & 0xFF;
            if (b == '(' || b == ')' || b == '\\') {
                sb.append('\\').append((char) b);
            } else if (b < 32 || b > 126) {
                sb.append(String.format(Locale.US, "\\%03o", b));
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ estructura del archivo

    private byte[] ensamblar() {
        int total = paginas.size();
        for (int i = 0; i < total; i++) {
            pagina = paginas.get(i);
            texto(MARGEN, 24, "Servidor ELVIS · Universidad Santiago de Cali", 7.5, false, GRIS);
            String numero = "Página " + (i + 1) + " de " + total;
            texto(ANCHO_PAGINA - MARGEN - ancho(numero, false, 7.5), 24, numero, 7.5, false, GRIS);
        }

        int objetoInfo = 5 + 2 * total;
        int cantidadObjetos = objetoInfo;
        int[] posiciones = new int[cantidadObjetos + 1];
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        escribir(out, "%PDF-1.4\n");
        out.write(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'}, 0, 6);

        posiciones[1] = out.size();
        escribir(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder hijos = new StringBuilder();
        for (int i = 0; i < total; i++) {
            hijos.append(5 + 2 * i).append(" 0 R ");
        }
        posiciones[2] = out.size();
        escribir(out, "2 0 obj\n<< /Type /Pages /Kids [ " + hijos + "] /Count " + total + " >>\nendobj\n");

        posiciones[3] = out.size();
        escribir(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");
        posiciones[4] = out.size();
        escribir(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");

        for (int i = 0; i < total; i++) {
            int objPagina = 5 + 2 * i;
            int objContenido = 6 + 2 * i;
            String contenido = paginas.get(i).toString();

            posiciones[objPagina] = out.size();
            escribir(out, objPagina + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                    + (int) ANCHO_PAGINA + " " + (int) ALTO_PAGINA + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + objContenido + " 0 R >>\nendobj\n");

            posiciones[objContenido] = out.size();
            escribir(out, objContenido + " 0 obj\n<< /Length " + contenido.length() + " >>\nstream\n"
                    + contenido + "endstream\nendobj\n");
        }

        posiciones[objetoInfo] = out.size();
        String fechaCreacion = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US).format(LocalDateTime.now());
        escribir(out, objetoInfo + " 0 obj\n<< /Title (" + escapar(datos.getTitulo())
                + ") /Producer (Servidor ELVIS - USC) /CreationDate (D:" + fechaCreacion + ") >>\nendobj\n");

        int inicioXref = out.size();
        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 ").append(cantidadObjetos + 1).append("\n");
        xref.append("0000000000 65535 f \n");
        for (int i = 1; i <= cantidadObjetos; i++) {
            xref.append(String.format(Locale.US, "%010d 00000 n \n", posiciones[i]));
        }
        xref.append("trailer\n<< /Size ").append(cantidadObjetos + 1)
                .append(" /Root 1 0 R /Info ").append(objetoInfo).append(" 0 R >>\n");
        xref.append("startxref\n").append(inicioXref).append("\n%%EOF\n");
        escribir(out, xref.toString());
        return out.toByteArray();
    }

    private static void escribir(ByteArrayOutputStream out, String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.ISO_8859_1);
        out.write(bytes, 0, bytes.length);
    }
}