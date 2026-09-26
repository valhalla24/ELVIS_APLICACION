package co.edu.usc.elvis.controller;

import co.edu.usc.elvis.dao.AsignacionDAO;
import co.edu.usc.elvis.dao.CursoDAO;
import co.edu.usc.elvis.dao.EstudianteDAO;
import co.edu.usc.elvis.dao.LicenciaDAO;
import co.edu.usc.elvis.dao.LogAuditoriaDAO;
import co.edu.usc.elvis.model.Asignacion;
import co.edu.usc.elvis.model.AsignacionDetalle;
import co.edu.usc.elvis.model.Curso;
import co.edu.usc.elvis.model.Estudiante;
import co.edu.usc.elvis.model.Licencia;
import co.edu.usc.elvis.util.EmailService;
import co.edu.usc.elvis.util.ImportadorEstudiantesExcel;
import co.edu.usc.elvis.util.ImportadorLicenciasExcel;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

public class AdministrarCuentasController {

    @FXML private TitledPane panelRegistrarCurso;
    @FXML private TextField campoNombreMateria;
    @FXML private TextField campoCodigoCurso;
    @FXML private TextField campoSemestre;
    @FXML private TextField campoDocente;
    @FXML private ComboBox<String> comboDia;
    @FXML private ComboBox<String> comboHoraInicio;
    @FXML private ComboBox<String> comboHoraFin;
    @FXML private Spinner<Integer> spinnerLicencias;
    @FXML private Button botonCrearCurso;
    @FXML private Label etiquetaEstado;
    @FXML private TableView<Curso> tablaCursos;
    @FXML private TableColumn<Curso, String> colCursoCodigo;
    @FXML private TableColumn<Curso, String> colCursoNombre;
    @FXML private TableColumn<Curso, String> colCursoDocente;
    @FXML private TableColumn<Curso, Void> colCursoAcciones;

    @FXML private TitledPane panelCargarLicencias;
    @FXML private Label etiquetaEstadoLicencias;
    @FXML private Button botonSeleccionarArchivoLicencias;
    @FXML private Button botonCargarLicencias;
    @FXML private Label etiquetaArchivoLicencias;
    @FXML private Label etiquetaResultadoLicencias;
    @FXML private TableView<Licencia> tablaLicencias;
    @FXML private TableColumn<Licencia, String> colLicUsuario;
    @FXML private TableColumn<Licencia, String> colLicIp;
    @FXML private TableColumn<Licencia, String> colLicEstado;
    @FXML private TableColumn<Licencia, Void> colLicAcciones;

    @FXML private TitledPane panelCargaListas;
    @FXML private ComboBox<Curso> comboCursoDestino;
    @FXML private Button botonSeleccionarArchivoEstudiantes;
    @FXML private Label etiquetaArchivoEstudiantes;
    @FXML private Label etiquetaResumenPreview;
    @FXML private TableView<Estudiante> tablaPreviewEstudiantes;
    @FXML private TableColumn<Estudiante, String> colNombre;
    @FXML private TableColumn<Estudiante, String> colCodigo;
    @FXML private TableColumn<Estudiante, String> colCorreo;
    @FXML private Button botonValidarYProcesar;
    @FXML private Label etiquetaResultadoEstudiantes;
    @FXML private TableView<Estudiante> tablaEstudiantesCurso;
    @FXML private TableColumn<Estudiante, String> colEstNombre;
    @FXML private TableColumn<Estudiante, String> colEstCodigo;
    @FXML private TableColumn<Estudiante, Void> colEstAcciones;

    @FXML private ComboBox<Curso> comboCursoAsignar;
    @FXML private Label etiquetaInfoCursoAsignar;
    @FXML private Button botonAsignarLicencias;
    @FXML private Label etiquetaResultadoAsignacion;
    @FXML private TableView<AsignacionDetalle> tablaAsignaciones;
    @FXML private TableColumn<AsignacionDetalle, String> colAsigCurso;
    @FXML private TableColumn<AsignacionDetalle, String> colAsigEstudiante;
    @FXML private TableColumn<AsignacionDetalle, String> colAsigLicencia;
    @FXML private TableColumn<AsignacionDetalle, String> colAsigHorario;
    @FXML private TableColumn<AsignacionDetalle, String> colAsigEstadoEnvio;
    @FXML private TableColumn<AsignacionDetalle, Void> colAsigAcciones;
    @FXML private Button botonConfirmarYEnviar;
    @FXML private Label etiquetaResultadoEnvio;

    private final EmailService emailService = new EmailService();

    private final CursoDAO cursoDAO = new CursoDAO();
    private final LicenciaDAO licenciaDAO = new LicenciaDAO();
    private final EstudianteDAO estudianteDAO = new EstudianteDAO();
    private final LogAuditoriaDAO logAuditoriaDAO = new LogAuditoriaDAO();
    private final AsignacionDAO asignacionDAO = new AsignacionDAO();
    private final ImportadorLicenciasExcel importadorLicencias = new ImportadorLicenciasExcel();
    private final ImportadorEstudiantesExcel importadorEstudiantes = new ImportadorEstudiantesExcel();
    private final ObservableList<Estudiante> estudiantesPreview = FXCollections.observableArrayList();
    private final ObservableList<Curso> cursosRegistrados = FXCollections.observableArrayList();
    private final ObservableList<Licencia> licenciasRegistradas = FXCollections.observableArrayList();
    private final ObservableList<Estudiante> estudiantesDelCurso = FXCollections.observableArrayList();
    private final ObservableList<AsignacionDetalle> asignacionesVista = FXCollections.observableArrayList();
    private File archivoLicenciasSeleccionado;
    private Curso cursoEnEdicion;

    private static final String ACCION_CARGA_LICENCIAS = "carga_licencias";
    private static final String ACCION_CARGA_ESTUDIANTES = "carga_estudiantes";
    private static final String ACCION_ASIGNACION = "asignacion_licencias";

    @FXML
    public void initialize() {
        comboDia.getItems().addAll("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado");

        List<String> horas = generarFranjasHorarias();
        comboHoraInicio.getItems().addAll(horas);
        comboHoraFin.getItems().addAll(horas);

        spinnerLicencias.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 25));

        actualizarEstadoLicencias();
        inicializarPanelEstudiantes();
        inicializarTablaCursos();
        inicializarTablaLicencias();
        inicializarPanelAsignacion();
    }

    private void inicializarPanelAsignacion() {
        comboCursoAsignar.getItems().setAll(cursoDAO.obtenerTodos());
        comboCursoAsignar.setConverter(new StringConverter<Curso>() {
            @Override
            public String toString(Curso curso) {
                return curso == null ? "" : curso.getCodigoCurso() + " - " + curso.getNombreMateria();
            }

            @Override
            public Curso fromString(String texto) {
                return null;
            }
        });
        comboCursoAsignar.valueProperty().addListener((obs, viejo, nuevo) -> mostrarInfoCursoAsignar(nuevo));

        colAsigCurso.setCellValueFactory(new PropertyValueFactory<>("cursoNombre"));
        colAsigEstudiante.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getEstudianteNombre() == null ? "(sin estudiante)" : datos.getValue().getEstudianteNombre()));
        colAsigLicencia.setCellValueFactory(new PropertyValueFactory<>("licenciaUsuario"));
        colAsigHorario.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().getDiaFranja() + ", " + datos.getValue().getHoraInicio()
                        + " - " + datos.getValue().getHoraFin()));
        colAsigEstadoEnvio.setCellValueFactory(new PropertyValueFactory<>("estadoEnvio"));
        colAsigAcciones.setCellFactory(columna -> crearCeldaAccionesAsignacion());
        tablaAsignaciones.setItems(asignacionesVista);
        cargarTablaAsignaciones();
    }

    private void mostrarInfoCursoAsignar(Curso curso) {
        if (curso == null) {
            etiquetaInfoCursoAsignar.setText("");
            return;
        }
        int yaAsignadas = asignacionDAO.obtenerPorCurso(curso.getId()).size();
        int estudiantesEnCurso = estudianteDAO.contarEstudiantesPorCurso(curso.getId());
        String info = curso.getDiaClase() + ", " + curso.getHoraInicio() + " - " + curso.getHoraFin()
                + " · Requiere " + curso.getLicenciasRequeridas() + " licencias · " + estudiantesEnCurso + " estudiante(s) en el curso"
                + (yaAsignadas > 0 ? " · Ya tiene " + yaAsignadas + " asignada(s)" : " · Sin asignar todavía");
        etiquetaInfoCursoAsignar.setText(info);
    }

    @FXML
    private void asignarLicenciasCurso() {
        Curso curso = comboCursoAsignar.getValue();
        if (curso == null) {
            mostrarMensaje(etiquetaResultadoAsignacion, "Selecciona un curso primero.", false);
            return;
        }

        List<Asignacion> existentes = asignacionDAO.obtenerPorCurso(curso.getId());
        if (!existentes.isEmpty()) {
            mostrarMensaje(etiquetaResultadoAsignacion,
                    "Este curso ya tiene " + existentes.size()
                            + " licencia(s) asignada(s). Libéralas primero si quieres reasignar.", false);
            return;
        }

        List<Estudiante> estudiantesDelCurso = estudianteDAO.obtenerPorCurso(curso.getId());
        if (estudiantesDelCurso.isEmpty()) {
            mostrarMensaje(etiquetaResultadoAsignacion,
                    "Este curso todavía no tiene estudiantes cargados (usa el panel \"Carga de Listas\").", false);
            return;
        }

        List<Asignacion> asignacionesDelDia = asignacionDAO.obtenerAsignacionesPorDia(curso.getDiaClase());
        Set<Integer> licenciasOcupadas = new HashSet<>();
        for (Asignacion a : asignacionesDelDia) {
            if (seSuperponen(a.getHoraInicio(), a.getHoraFin(), curso.getHoraInicio(), curso.getHoraFin())) {
                licenciasOcupadas.add(a.getLicenciaId());
            }
        }

        List<Licencia> disponiblesEnFranja = new ArrayList<>();
        for (Licencia l : licenciaDAO.obtenerTodas()) {
            if (!licenciasOcupadas.contains(l.getId())) {
                disponiblesEnFranja.add(l);
            }
        }

        int requeridas = curso.getLicenciasRequeridas();
        if (disponiblesEnFranja.isEmpty()) {
            mostrarMensaje(etiquetaResultadoAsignacion,
                    "No hay licencias libres en ese horario. Se necesitan " + requeridas + ", hay 0 disponibles.", false);
            return;
        }

        int aAsignar = Math.min(requeridas, Math.min(disponiblesEnFranja.size(), estudiantesDelCurso.size()));

        try {
            for (int i = 0; i < aAsignar; i++) {
                Licencia l = disponiblesEnFranja.get(i);
                Estudiante est = estudiantesDelCurso.get(i);
                asignacionDAO.crearAsignacion(l.getId(), curso.getId(), est.getId(), curso.getDiaClase(),
                        curso.getHoraInicio(), curso.getHoraFin());
                licenciaDAO.recalcularEstado(l.getId(), asignacionDAO.contarAsignacionesPorLicencia(l.getId()));
            }

            String mensaje = aAsignar + " licencia(s) asignada(s) a estudiantes del curso \"" + curso.getNombreMateria() + "\".";
            if (aAsignar < estudiantesDelCurso.size()) {
                mensaje += " " + (estudiantesDelCurso.size() - aAsignar) + " estudiante(s) se quedaron sin licencia por falta de cupo.";
            }
            if (aAsignar < requeridas && disponiblesEnFranja.size() < requeridas) {
                mensaje += " (Solo había " + disponiblesEnFranja.size() + " licencia(s) libres en ese horario, se pedían " + requeridas + ".)";
            }
            mostrarMensaje(etiquetaResultadoAsignacion, mensaje, aAsignar == Math.min(requeridas, estudiantesDelCurso.size()));

            logAuditoriaDAO.registrarEvento(ACCION_ASIGNACION,
                    aAsignar + " licencias asignadas a estudiantes (curso: " + curso.getCodigoCurso() + ")", "admin");
            cargarTablaAsignaciones();
            cargarTablaLicencias();
            actualizarEstadoLicencias();
            mostrarInfoCursoAsignar(curso);
        } catch (SQLException e) {
            mostrarMensaje(etiquetaResultadoAsignacion, "Error al asignar: " + e.getMessage(), false);
        }
    }

    private void cargarTablaAsignaciones() {
        asignacionesVista.setAll(asignacionDAO.obtenerTodasConDetalle());
    }

    private TableCell<AsignacionDetalle, Void> crearCeldaAccionesAsignacion() {
        return new TableCell<AsignacionDetalle, Void>() {
            private final Button botonReenviar = new Button("Reenviar");
            private final Button botonLiberar = new Button("Liberar");
            private final HBox contenedor = new HBox(6, botonReenviar, botonLiberar);

            {
                botonReenviar.getStyleClass().add("boton-tabla");
                botonLiberar.getStyleClass().add("boton-tabla-peligro");
                botonReenviar.setOnAction(e -> reenviarNotificacion(getTableView().getItems().get(getIndex())));
                botonLiberar.setOnAction(e -> liberarAsignacion(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AsignacionDetalle detalle = getTableView().getItems().get(getIndex());
                    botonReenviar.setDisable(detalle.getEstudianteCorreo() == null || detalle.getEstudianteCorreo().isEmpty());
                    setGraphic(contenedor);
                }
            }
        };
    }

    private void reenviarNotificacion(AsignacionDetalle detalle) {
        if (detalle.getEstudianteCorreo() == null || detalle.getEstudianteCorreo().isEmpty()) {
            mostrarMensaje(etiquetaResultadoEnvio, "Este estudiante no tiene correo registrado.", false);
            return;
        }
        try {
            emailService.enviarCredenciales(detalle);
            asignacionDAO.marcarNotificado(detalle.getId());
            cargarTablaAsignaciones();
            mostrarMensaje(etiquetaResultadoEnvio,
                    "Correo reenviado a " + detalle.getEstudianteNombre() + " (" + detalle.getEstudianteCorreo() + ").", true);
        } catch (Exception e) {
            mostrarMensaje(etiquetaResultadoEnvio, "No se pudo enviar el correo: " + e.getMessage(), false);
        }
    }

    @FXML
    private void confirmarYEnviarNotificaciones() {
        List<AsignacionDetalle> pendientes = asignacionDAO.obtenerPendientesDeNotificar();
        if (pendientes.isEmpty()) {
            mostrarMensaje(etiquetaResultadoEnvio, "No hay correos pendientes de enviar.", true);
            return;
        }

        int enviados = 0;
        int fallidos = 0;
        for (AsignacionDetalle detalle : pendientes) {
            if (detalle.getEstudianteCorreo() == null || detalle.getEstudianteCorreo().isEmpty()) {
                fallidos++;
                continue;
            }
            try {
                emailService.enviarCredenciales(detalle);
                asignacionDAO.marcarNotificado(detalle.getId());
                enviados++;
            } catch (Exception e) {
                fallidos++;
            }
        }

        String mensaje = enviados + " correo(s) enviado(s)";
        if (fallidos > 0) {
            mensaje += ", " + fallidos + " con error";
        }
        mostrarMensaje(etiquetaResultadoEnvio, mensaje + ".", fallidos == 0);
        logAuditoriaDAO.registrarEvento("envio_credenciales", mensaje, "admin");
        cargarTablaAsignaciones();
    }

    private void liberarAsignacion(AsignacionDetalle detalle) {
        try {
            asignacionDAO.eliminarAsignacion(detalle.getId());
            licenciaDAO.recalcularEstado(detalle.getLicenciaId(),
                    asignacionDAO.contarAsignacionesPorLicencia(detalle.getLicenciaId()));
            cargarTablaAsignaciones();
            cargarTablaLicencias();
            actualizarEstadoLicencias();
            mostrarMensaje(etiquetaResultadoAsignacion,
                    "Licencia \"" + detalle.getLicenciaUsuario() + "\" liberada del curso \""
                            + detalle.getCursoNombre() + "\".", true);
            if (comboCursoAsignar.getValue() != null && comboCursoAsignar.getValue().getId() == detalle.getCursoId()) {
                mostrarInfoCursoAsignar(comboCursoAsignar.getValue());
            }
        } catch (SQLException e) {
            mostrarMensaje(etiquetaResultadoAsignacion, "Error al liberar: " + e.getMessage(), false);
        }
    }

    private boolean seSuperponen(String inicio1, String fin1, String inicio2, String fin2) {
        return co.edu.usc.elvis.util.HorarioUtil.seSuperponen(inicio1, fin1, inicio2, fin2);
    }

    private void inicializarTablaLicencias() {
        colLicUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colLicIp.setCellValueFactory(new PropertyValueFactory<>("ipAsignada"));
        colLicEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colLicAcciones.setCellFactory(columna -> crearCeldaAccionesLicencia());
        tablaLicencias.setItems(licenciasRegistradas);
        cargarTablaLicencias();
    }

    private void cargarTablaLicencias() {
        licenciasRegistradas.setAll(licenciaDAO.obtenerTodas());
    }

    private TableCell<Licencia, Void> crearCeldaAccionesLicencia() {
        return new TableCell<Licencia, Void>() {
            private final Button botonEditar = new Button("Editar");
            private final Button botonEliminar = new Button("Eliminar");
            private final HBox contenedor = new HBox(6, botonEditar, botonEliminar);

            {
                botonEditar.getStyleClass().add("boton-tabla");
                botonEliminar.getStyleClass().add("boton-tabla-peligro");
                botonEditar.setOnAction(e -> editarLicenciaDialogo(getTableView().getItems().get(getIndex())));
                botonEliminar.setOnAction(e -> confirmarYEliminarLicencia(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : contenedor);
            }
        };
    }

    private void editarLicenciaDialogo(Licencia licencia) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Editar Licencia");
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField campoUsuario = new TextField(licencia.getUsuario());
        TextField campoContrasena = new TextField(licencia.getContrasena());
        TextField campoIp = new TextField(licencia.getIpAsignada());

        VBox contenido = new VBox(8,
                new Label("Usuario:"), campoUsuario,
                new Label("Contraseña:"), campoContrasena,
                new Label("IP:"), campoIp);
        contenido.setPadding(new Insets(10));
        dialogo.getDialogPane().setContent(contenido);

        dialogo.showAndWait().ifPresent(resultado -> {
            if (resultado == ButtonType.OK) {
                String nuevoUsuario = campoUsuario.getText().trim();
                if (nuevoUsuario.isEmpty()) {
                    return;
                }
                try {
                    licenciaDAO.actualizarLicenciaPorId(licencia.getId(), nuevoUsuario,
                            campoContrasena.getText().trim(), campoIp.getText().trim());
                    cargarTablaLicencias();
                } catch (SQLException e) {
                    mostrarMensaje(etiquetaResultadoLicencias, "Error al actualizar la licencia: " + e.getMessage(), false);
                }
            }
        });
    }

    private void confirmarYEliminarLicencia(Licencia licencia) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Seguro que quieres eliminar la licencia \"" + licencia.getUsuario() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Confirmar eliminación");

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.YES) {
                try {
                    licenciaDAO.eliminarLicencia(licencia.getId());
                    cargarTablaLicencias();
                    actualizarEstadoLicencias();
                } catch (SQLException e) {
                    mostrarMensaje(etiquetaResultadoLicencias,
                            "No se pudo eliminar: es posible que la licencia tenga asignaciones activas.", false);
                }
            }
        });
    }

    private void inicializarTablaCursos() {
        colCursoCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoCurso"));
        colCursoNombre.setCellValueFactory(new PropertyValueFactory<>("nombreMateria"));
        colCursoDocente.setCellValueFactory(new PropertyValueFactory<>("docenteResponsable"));
        colCursoAcciones.setCellFactory(columna -> crearCeldaAcciones());
        tablaCursos.setItems(cursosRegistrados);
        cargarTablaCursos();
    }

    private TableCell<Curso, Void> crearCeldaAcciones() {
        return new TableCell<Curso, Void>() {
            private final Button botonEditar = new Button("Editar");
            private final Button botonEliminar = new Button("Eliminar");
            private final HBox contenedor = new HBox(6, botonEditar, botonEliminar);

            {
                botonEditar.getStyleClass().add("boton-tabla");
                botonEliminar.getStyleClass().add("boton-tabla-peligro");
                botonEditar.setOnAction(e -> cargarCursoParaEditar(getTableView().getItems().get(getIndex())));
                botonEliminar.setOnAction(e -> confirmarYEliminarCurso(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : contenedor);
            }
        };
    }

    private void cargarTablaCursos() {
        cursosRegistrados.setAll(cursoDAO.obtenerTodos());
    }

    private void cargarCursoParaEditar(Curso curso) {
        cursoEnEdicion = curso;
        campoNombreMateria.setText(curso.getNombreMateria());
        campoCodigoCurso.setText(curso.getCodigoCurso());
        campoSemestre.setText(curso.getSemestreLectivo());
        campoDocente.setText(curso.getDocenteResponsable());
        comboDia.setValue(curso.getDiaClase());
        comboHoraInicio.setValue(curso.getHoraInicio());
        comboHoraFin.setValue(curso.getHoraFin());
        spinnerLicencias.getValueFactory().setValue(curso.getLicenciasRequeridas());
        botonCrearCurso.setText("Actualizar Curso");
        panelRegistrarCurso.setExpanded(true);
        mostrarMensaje(etiquetaEstado, "Editando \"" + curso.getNombreMateria() + "\" — modifica los campos y guarda.", true);
    }

    private void confirmarYEliminarCurso(Curso curso) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Seguro que quieres eliminar el curso \"" + curso.getNombreMateria()
                        + "\" (" + curso.getCodigoCurso() + ")?",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Confirmar eliminación");

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.YES) {
                try {
                    cursoDAO.eliminarCurso(curso.getId());
                    mostrarMensaje(etiquetaEstado, "Curso \"" + curso.getNombreMateria() + "\" eliminado.", true);
                    cargarTablaCursos();
                    comboCursoDestino.getItems().setAll(cursoDAO.obtenerTodos());
                } catch (SQLException e) {
                    mostrarMensaje(etiquetaEstado,
                            "No se pudo eliminar: es posible que el curso tenga estudiantes o licencias asociadas.",
                            false);
                }
            }
        });
    }

    private void inicializarPanelEstudiantes() {
        comboCursoDestino.getItems().setAll(cursoDAO.obtenerTodos());
        comboCursoDestino.setConverter(new StringConverter<Curso>() {
            @Override
            public String toString(Curso curso) {
                return curso == null ? "" : curso.getCodigoCurso() + " - " + curso.getNombreMateria();
            }

            @Override
            public Curso fromString(String texto) {
                return null;
            }
        });
        comboCursoDestino.valueProperty().addListener((obs, anterior, nuevo) -> {
            botonValidarYProcesar.setDisable(estudiantesPreview.isEmpty() || nuevo == null);
            cargarTablaEstudiantesCurso();
        });

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoEstudiantil"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correoInstitucional"));
        tablaPreviewEstudiantes.setItems(estudiantesPreview);

        colEstNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colEstCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoEstudiantil"));
        colEstAcciones.setCellFactory(columna -> crearCeldaAccionesEstudiante());
        tablaEstudiantesCurso.setItems(estudiantesDelCurso);
    }

    private void cargarTablaEstudiantesCurso() {
        Curso curso = comboCursoDestino.getValue();
        estudiantesDelCurso.setAll(curso == null ? Collections.emptyList() : estudianteDAO.obtenerPorCurso(curso.getId()));
    }

    private TableCell<Estudiante, Void> crearCeldaAccionesEstudiante() {
        return new TableCell<Estudiante, Void>() {
            private final Button botonEditar = new Button("Editar");
            private final Button botonEliminar = new Button("Eliminar");
            private final HBox contenedor = new HBox(6, botonEditar, botonEliminar);

            {
                botonEditar.getStyleClass().add("boton-tabla");
                botonEliminar.getStyleClass().add("boton-tabla-peligro");
                botonEditar.setOnAction(e -> editarEstudianteDialogo(getTableView().getItems().get(getIndex())));
                botonEliminar.setOnAction(e -> confirmarYEliminarEstudiante(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : contenedor);
            }
        };
    }

    private void editarEstudianteDialogo(Estudiante estudiante) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Editar Estudiante");
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField campoNombreDlg = new TextField(estudiante.getNombreCompleto());
        TextField campoCodigoDlg = new TextField(estudiante.getCodigoEstudiantil());
        TextField campoCorreoDlg = new TextField(estudiante.getCorreoInstitucional());

        VBox contenido = new VBox(8,
                new Label("Nombre completo:"), campoNombreDlg,
                new Label("Código:"), campoCodigoDlg,
                new Label("Correo institucional:"), campoCorreoDlg);
        contenido.setPadding(new Insets(10));
        dialogo.getDialogPane().setContent(contenido);

        dialogo.showAndWait().ifPresent(resultado -> {
            if (resultado == ButtonType.OK) {
                String nuevoNombre = campoNombreDlg.getText().trim();
                String nuevoCodigo = campoCodigoDlg.getText().trim();
                if (nuevoNombre.isEmpty() || nuevoCodigo.isEmpty()) {
                    return;
                }
                try {
                    estudianteDAO.actualizarEstudiantePorId(estudiante.getId(), nuevoNombre, nuevoCodigo,
                            campoCorreoDlg.getText().trim());
                    cargarTablaEstudiantesCurso();
                } catch (SQLException e) {
                    mostrarMensaje(etiquetaResultadoEstudiantes, "Error al actualizar: " + e.getMessage(), false);
                }
            }
        });
    }

    private void confirmarYEliminarEstudiante(Estudiante estudiante) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Seguro que quieres eliminar a \"" + estudiante.getNombreCompleto() + "\" de este curso?",
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Confirmar eliminación");

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.YES) {
                try {
                    estudianteDAO.eliminarEstudiante(estudiante.getId());
                    cargarTablaEstudiantesCurso();
                } catch (SQLException e) {
                    mostrarMensaje(etiquetaResultadoEstudiantes,
                            "No se pudo eliminar: es posible que tenga una licencia asignada.", false);
                }
            }
        });
    }

    private List<String> generarFranjasHorarias() {
        return co.edu.usc.elvis.util.HorarioUtil.generarFranjasHorarias();
    }

    @FXML
    private void registrarCurso() {
        String nombreMateria = campoNombreMateria.getText().trim();
        String codigoCurso = campoCodigoCurso.getText().trim();
        String semestre = campoSemestre.getText().trim();
        String docente = campoDocente.getText().trim();
        String dia = comboDia.getValue();
        String horaInicio = comboHoraInicio.getValue();
        String horaFin = comboHoraFin.getValue();
        Integer licencias = spinnerLicencias.getValue();

        if (nombreMateria.isEmpty() || codigoCurso.isEmpty() || semestre.isEmpty()
                || docente.isEmpty() || dia == null || horaInicio == null || horaFin == null) {
            mostrarMensaje(etiquetaEstado, "Completa todos los campos antes de guardar el curso.", false);
            return;
        }

        boolean editando = cursoEnEdicion != null;
        boolean codigoDuplicado = editando
                ? cursoDAO.existeCodigoCursoExcluyendo(codigoCurso, cursoEnEdicion.getId())
                : cursoDAO.existeCodigoCurso(codigoCurso);

        if (codigoDuplicado) {
            mostrarMensaje(etiquetaEstado, "Ya existe otro curso registrado con el código " + codigoCurso + ".", false);
            return;
        }

        try {
            if (editando) {
                cursoEnEdicion.setNombreMateria(nombreMateria);
                cursoEnEdicion.setCodigoCurso(codigoCurso);
                cursoEnEdicion.setSemestreLectivo(semestre);
                cursoEnEdicion.setDocenteResponsable(docente);
                cursoEnEdicion.setDiaClase(dia);
                cursoEnEdicion.setHoraInicio(horaInicio);
                cursoEnEdicion.setHoraFin(horaFin);
                cursoEnEdicion.setLicenciasRequeridas(licencias);
                cursoDAO.actualizarCurso(cursoEnEdicion);
                mostrarMensaje(etiquetaEstado, "Curso \"" + nombreMateria + "\" actualizado correctamente.", true);
            } else {
                Curso nuevoCurso = new Curso(nombreMateria, codigoCurso, semestre, docente,
                        dia, horaInicio, horaFin, licencias);
                cursoDAO.registrarCurso(nuevoCurso);
                mostrarMensaje(etiquetaEstado, "Curso \"" + nombreMateria + "\" registrado correctamente.", true);
            }
            limpiarFormularioCurso();
            cursoEnEdicion = null;
            botonCrearCurso.setText("Crear y Registrar Curso");
            panelRegistrarCurso.setExpanded(false);
            cargarTablaCursos();
            comboCursoDestino.getItems().setAll(cursoDAO.obtenerTodos());
        } catch (SQLException e) {
            mostrarMensaje(etiquetaEstado, "Error al guardar el curso: " + e.getMessage(), false);
        }
    }

    @FXML
    private void seleccionarArchivoLicencias() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Selecciona el archivo Excel de licencias");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));

        Window ventana = botonSeleccionarArchivoLicencias.getScene().getWindow();
        File archivo = selector.showOpenDialog(ventana);

        if (archivo != null) {
            archivoLicenciasSeleccionado = archivo;
            etiquetaArchivoLicencias.setText(archivo.getName());
            botonCargarLicencias.setDisable(false);
        }
    }

    @FXML
    private void cargarLicencias() {
        if (archivoLicenciasSeleccionado == null) {
            return;
        }

        try {
            ImportadorLicenciasExcel.ResultadoImportacion resultado =
                    importadorLicencias.importar(archivoLicenciasSeleccionado);

            String mensaje = resultado.agregadas + " licencias nuevas, "
                    + resultado.actualizadas + " actualizadas";
            if (resultado.conError > 0) {
                mensaje += ", " + resultado.conError + " con error";
            }
            mostrarMensaje(etiquetaResultadoLicencias, mensaje + ".", resultado.conError == 0);

            String detalleLog = mensaje + " (archivo: " + archivoLicenciasSeleccionado.getName() + ")";
            logAuditoriaDAO.registrarEvento(ACCION_CARGA_LICENCIAS, detalleLog, "admin");
            actualizarEstadoLicencias();
            cargarTablaLicencias();
            panelCargarLicencias.setExpanded(false);
        } catch (IOException e) {
            mostrarMensaje(etiquetaResultadoLicencias, "No se pudo leer el archivo: " + e.getMessage(), false);
        }
    }

    private void actualizarEstadoLicencias() {
        int total = licenciaDAO.contarLicenciasTotales();
        LogAuditoriaDAO.EventoReciente ultimaCarga = logAuditoriaDAO.obtenerUltimoEvento(ACCION_CARGA_LICENCIAS);

        StringBuilder texto = new StringBuilder();
        if (total == 0) {
            texto.append("Aún no hay licencias cargadas en el sistema.");
        } else {
            texto.append("Actualmente hay ").append(total).append(" licencias en el sistema.");
        }
        if (ultimaCarga != null) {
            texto.append(" Última carga: ").append(ultimaCarga.fechaHora)
                    .append(" — ").append(ultimaCarga.detalle);
        }
        etiquetaEstadoLicencias.setText(texto.toString());
    }

    private void mostrarMensaje(Label etiqueta, String mensaje, boolean exito) {
        etiqueta.setText(mensaje);
        etiqueta.getStyleClass().removeAll("mensaje-exito", "mensaje-error");
        etiqueta.getStyleClass().add(exito ? "mensaje-exito" : "mensaje-error");
    }

    private void limpiarFormularioCurso() {
        campoNombreMateria.clear();
        campoCodigoCurso.clear();
        campoSemestre.clear();
        campoDocente.clear();
        comboDia.setValue(null);
        comboHoraInicio.setValue(null);
        comboHoraFin.setValue(null);
        spinnerLicencias.getValueFactory().setValue(25);
    }

    @FXML
    private void seleccionarArchivoEstudiantes() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Selecciona el archivo Excel de estudiantes");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));

        Window ventana = botonSeleccionarArchivoEstudiantes.getScene().getWindow();
        File archivo = selector.showOpenDialog(ventana);

        if (archivo == null) {
            return;
        }

        etiquetaArchivoEstudiantes.setText(archivo.getName());
        etiquetaResultadoEstudiantes.setText("");

        try {
            List<Estudiante> filas = importadorEstudiantes.leer(archivo);
            estudiantesPreview.setAll(filas);
            mostrarMensaje(etiquetaResumenPreview, filas.size() + " estudiantes detectados en el archivo.", true);
            botonValidarYProcesar.setDisable(filas.isEmpty() || comboCursoDestino.getValue() == null);
        } catch (IOException e) {
            estudiantesPreview.clear();
            botonValidarYProcesar.setDisable(true);
            mostrarMensaje(etiquetaResumenPreview, "No se pudo leer el archivo: " + e.getMessage(), false);
        }
    }

    @FXML
    private void procesarListaEstudiantes() {
        Curso cursoSeleccionado = comboCursoDestino.getValue();
        if (cursoSeleccionado == null || estudiantesPreview.isEmpty()) {
            return;
        }

        int nuevos = 0;
        int actualizados = 0;
        int errores = 0;

        for (Estudiante fila : estudiantesPreview) {
            if (fila.getCodigoEstudiantil() == null || fila.getCodigoEstudiantil().isEmpty()) {
                errores++;
                continue;
            }
            fila.setCursoId(cursoSeleccionado.getId());
            try {
                if (estudianteDAO.existeCodigoEstudiantil(fila.getCodigoEstudiantil())) {
                    estudianteDAO.actualizarEstudiante(fila);
                    actualizados++;
                } else {
                    estudianteDAO.insertarEstudiante(fila);
                    nuevos++;
                }
            } catch (SQLException e) {
                errores++;
            }
        }

        String mensaje = nuevos + " estudiantes nuevos, " + actualizados + " actualizados";
        if (errores > 0) {
            mensaje += ", " + errores + " con error";
        }
        mostrarMensaje(etiquetaResultadoEstudiantes,
                mensaje + " en el curso " + cursoSeleccionado.getNombreMateria() + ".", errores == 0);

        String detalleLog = mensaje + " (curso: " + cursoSeleccionado.getCodigoCurso() + ")";
        logAuditoriaDAO.registrarEvento(ACCION_CARGA_ESTUDIANTES, detalleLog, "admin");
        cargarTablaEstudiantesCurso();
        if (errores == 0) {
            panelCargaListas.setExpanded(false);
        }
    }
}