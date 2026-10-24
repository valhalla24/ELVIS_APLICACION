package co.edu.usc.elvis.controller;

import co.edu.usc.elvis.dao.LogAuditoriaDAO;
import co.edu.usc.elvis.model.ReporteDatos;
import co.edu.usc.elvis.model.Usuario;
import co.edu.usc.elvis.util.ExportadorExcel;
import co.edu.usc.elvis.util.ExportadorPdf;
import co.edu.usc.elvis.util.ReporteService;
import co.edu.usc.elvis.util.SesionActual;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Pantalla de reportes: el usuario elige el tipo, genera una vista previa y
 * la exporta a PDF o Excel. Un profesor solo obtiene datos de sus cursos y no
 * ve la bitacora de auditoria (esa es del administrador).
 */
public class ReportesController {

    public enum TipoReporte {
        OCUPACION_CURSOS("Ocupación de licencias por curso", false,
                "Foto del estado actual: no usa rango de fechas."),
        ASIGNACIONES("Asignaciones de licencias por estudiante", true,
                "El rango filtra por la fecha en que se asignó cada licencia. Déjalo vacío para incluir todo."),
        OCUPACION_FRANJAS("Ocupación por franja horaria", false,
                "Foto del estado actual de todos los cursos (igual que el calendario): no usa rango de fechas."),
        BITACORA("Bitácora de auditoría (solo administrador)", true,
                "El rango filtra por la fecha del evento. Déjalo vacío para incluir todo.");

        private final String etiqueta;
        private final boolean usaRango;
        private final String ayuda;

        TipoReporte(String etiqueta, boolean usaRango, String ayuda) {
            this.etiqueta = etiqueta;
            this.usaRango = usaRango;
            this.ayuda = ayuda;
        }

        public boolean usaRango() { return usaRango; }

        @Override
        public String toString() { return etiqueta; }
    }

    @FXML private ComboBox<TipoReporte> comboTipoReporte;
    @FXML private TextField campoDependencia;
    @FXML private DatePicker fechaDesde;
    @FXML private DatePicker fechaHasta;
    @FXML private Label etiquetaAyudaRango;
    @FXML private Button botonGenerar;
    @FXML private Button botonExportarPdf;
    @FXML private Button botonExportarExcel;
    @FXML private Label etiquetaResultado;
    @FXML private Label etiquetaTituloVista;
    @FXML private Label etiquetaResumen;
    @FXML private TableView<List<Object>> tablaVista;

    private final ReporteService reporteService = new ReporteService();
    private final LogAuditoriaDAO logAuditoriaDAO = new LogAuditoriaDAO();
    private ReporteDatos ultimoReporte;

    @FXML
    public void initialize() {
        campoDependencia.setText("Facultad de Ingeniería");

        comboTipoReporte.getItems().addAll(TipoReporte.OCUPACION_CURSOS, TipoReporte.ASIGNACIONES,
                TipoReporte.OCUPACION_FRANJAS);
        if (SesionActual.esAdministrador()) {
            comboTipoReporte.getItems().add(TipoReporte.BITACORA);
        }
        comboTipoReporte.valueProperty().addListener((obs, anterior, nuevo) -> alCambiarTipo(nuevo));
        comboTipoReporte.setValue(TipoReporte.OCUPACION_CURSOS);

        tablaVista.setPlaceholder(new Label("Elige un reporte y pulsa \"Generar vista previa\"."));
        limpiarVista();
    }

    private void alCambiarTipo(TipoReporte tipo) {
        boolean usaRango = tipo != null && tipo.usaRango();
        fechaDesde.setDisable(!usaRango);
        fechaHasta.setDisable(!usaRango);
        if (!usaRango) {
            fechaDesde.setValue(null);
            fechaHasta.setValue(null);
        }
        etiquetaAyudaRango.setText(tipo == null ? "" : tipo.ayuda);
        limpiarVista();
    }

    private void limpiarVista() {
        ultimoReporte = null;
        tablaVista.getColumns().clear();
        tablaVista.setItems(FXCollections.observableArrayList());
        etiquetaTituloVista.setText("");
        etiquetaResumen.setText("");
        botonExportarPdf.setDisable(true);
        botonExportarExcel.setDisable(true);
    }

    @FXML
    private void generarVistaPrevia() {
        Usuario usuario = SesionActual.obtenerUsuario();
        TipoReporte tipo = comboTipoReporte.getValue();
        if (usuario == null) {
            return;
        }
        if (tipo == null) {
            mostrarMensaje("Elige el tipo de reporte.", false);
            return;
        }
        if (tipo == TipoReporte.BITACORA && !usuario.esAdministrador()) {
            mostrarMensaje("La bitácora de auditoría es solo para el administrador.", false);
            return;
        }

        LocalDate desde = tipo.usaRango() ? fechaDesde.getValue() : null;
        LocalDate hasta = tipo.usaRango() ? fechaHasta.getValue() : null;
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            mostrarMensaje("La fecha \"Desde\" no puede ser posterior a \"Hasta\".", false);
            return;
        }
        String dependencia = campoDependencia.getText().trim();

        ReporteDatos datos;
        switch (tipo) {
            case OCUPACION_CURSOS:
                datos = reporteService.ocupacionPorCurso(usuario, dependencia);
                break;
            case ASIGNACIONES:
                datos = reporteService.asignaciones(usuario, dependencia, desde, hasta);
                break;
            case OCUPACION_FRANJAS:
                datos = reporteService.ocupacionPorFranja(usuario, dependencia);
                break;
            default:
                datos = reporteService.bitacora(usuario, dependencia, desde, hasta);
                break;
        }
        mostrarVista(datos);
    }

    private void mostrarVista(ReporteDatos datos) {
        ultimoReporte = datos;

        tablaVista.getColumns().clear();
        List<String> nombres = datos.getColumnas();
        for (int i = 0; i < nombres.size(); i++) {
            final int indice = i;
            TableColumn<List<Object>, String> columna = new TableColumn<>(nombres.get(i));
            columna.setCellValueFactory(celda -> new SimpleStringProperty(
                    ReporteDatos.formatear(celda.getValue().get(indice), datos.getTipos().get(indice))));
            columna.setPrefWidth(Math.max(90, datos.getPesos().get(i) * 90));
            tablaVista.getColumns().add(columna);
        }
        tablaVista.setItems(FXCollections.observableArrayList(datos.getFilas()));

        etiquetaTituloVista.setText(datos.getTitulo() + " — " + datos.getFilas().size() + " fila(s)");
        etiquetaResumen.setText(String.join("\n", datos.getResumen()));

        boolean hayDatos = !datos.getFilas().isEmpty();
        botonExportarPdf.setDisable(!hayDatos);
        botonExportarExcel.setDisable(!hayDatos);
        if (hayDatos) {
            mostrarMensaje("Vista previa lista. Ya puedes exportarla a PDF o Excel.", true);
        } else {
            mostrarMensaje("No hay datos para ese reporte (revisa el rango de fechas o si ya hay información cargada).", false);
        }
    }

    // ------------------------------------------------------------------ exportar

    @FXML
    private void exportarPdf() {
        File archivo = pedirArchivo("pdf", "PDF (*.pdf)");
        if (archivo != null) {
            guardarPdf(archivo);
        }
    }

    @FXML
    private void exportarExcel() {
        File archivo = pedirArchivo("xlsx", "Excel (*.xlsx)");
        if (archivo != null) {
            guardarExcel(archivo);
        }
    }

    private File pedirArchivo(String extension, String descripcion) {
        if (ultimoReporte == null) {
            return null;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter(descripcion, "*." + extension));
        selector.setInitialFileName("reporte_" + ultimoReporte.getCodigo() + "_"
                + DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now()) + "." + extension);
        Window ventana = botonGenerar.getScene().getWindow();
        File archivo = selector.showSaveDialog(ventana);
        if (archivo != null && !archivo.getName().toLowerCase().endsWith("." + extension)) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + "." + extension);
        }
        return archivo;
    }

    private void guardarPdf(File archivo) {
        if (ultimoReporte == null) {
            return;
        }
        try {
            ExportadorPdf.exportar(ultimoReporte, archivo);
            registrarExportacion("PDF", archivo);
            mostrarMensaje("PDF guardado en: " + archivo.getAbsolutePath(), true);
        } catch (IOException e) {
            mostrarMensaje("No se pudo guardar el PDF: " + e.getMessage(), false);
        }
    }

    private void guardarExcel(File archivo) {
        if (ultimoReporte == null) {
            return;
        }
        try {
            ExportadorExcel.exportar(ultimoReporte, archivo);
            registrarExportacion("Excel", archivo);
            mostrarMensaje("Excel guardado en: " + archivo.getAbsolutePath(), true);
        } catch (IOException e) {
            mostrarMensaje("No se pudo guardar el Excel: " + e.getMessage(), false);
        }
    }

    private void registrarExportacion(String formato, File archivo) {
        Usuario actual = SesionActual.obtenerUsuario();
        logAuditoriaDAO.registrarEvento("reporte_exportado",
                formato + ": " + ultimoReporte.getTitulo() + " (" + ultimoReporte.getFilas().size()
                        + " filas) → " + archivo.getName(),
                actual == null ? "desconocido" : actual.getNombreUsuario());
    }

    private void mostrarMensaje(String mensaje, boolean exito) {
        etiquetaResultado.setText(mensaje);
        etiquetaResultado.getStyleClass().removeAll("mensaje-exito", "mensaje-error");
        etiquetaResultado.getStyleClass().add(exito ? "mensaje-exito" : "mensaje-error");
    }
}