package co.edu.usc.elvis.controller;

import co.edu.usc.elvis.dao.CursoDAO;
import co.edu.usc.elvis.dao.LogAuditoriaDAO;
import co.edu.usc.elvis.dao.UsuarioDAO;
import co.edu.usc.elvis.model.Usuario;
import co.edu.usc.elvis.util.SesionActual;
import java.sql.SQLException;
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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Pantalla exclusiva del administrador: crear, cambiar de rol, restablecer
 * contraseña y eliminar las cuentas que pueden entrar al sistema.
 */
public class UsuariosController {

    @FXML private TextField campoNombreCompleto;
    @FXML private TextField campoNombreUsuario;
    @FXML private PasswordField campoContrasena;
    @FXML private ComboBox<String> comboRol;
    @FXML private Label etiquetaResultado;
    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, Void> colAcciones;

    private static final String ROL_TEXTO_PROFESOR = "Profesor";
    private static final String ROL_TEXTO_ADMIN = "Administrador";
    private static final int LARGO_MINIMO_CONTRASENA = co.edu.usc.elvis.util.SeguridadUtil.LARGO_MINIMO;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final CursoDAO cursoDAO = new CursoDAO();
    private final LogAuditoriaDAO logAuditoriaDAO = new LogAuditoriaDAO();
    private final ObservableList<Usuario> usuarios = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        comboRol.getItems().addAll(ROL_TEXTO_PROFESOR, ROL_TEXTO_ADMIN);
        comboRol.setValue(ROL_TEXTO_PROFESOR);

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("nombreUsuario"));
        colRol.setCellValueFactory(datos -> new SimpleStringProperty(
                datos.getValue().esAdministrador() ? ROL_TEXTO_ADMIN : ROL_TEXTO_PROFESOR));
        colAcciones.setCellFactory(columna -> crearCeldaAcciones());
        tablaUsuarios.setItems(usuarios);
        cargarTabla();
    }

    private void cargarTabla() {
        usuarios.setAll(usuarioDAO.obtenerTodos());
    }

    @FXML
    private void crearUsuario() {
        if (!SesionActual.esAdministrador()) {
            return;
        }

        String nombreCompleto = campoNombreCompleto.getText().trim();
        String nombreUsuario = campoNombreUsuario.getText().trim();
        String contrasena = campoContrasena.getText();
        String rolSeleccionado = comboRol.getValue();

        if (nombreCompleto.isEmpty() || nombreUsuario.isEmpty() || contrasena.isEmpty() || rolSeleccionado == null) {
            mostrarMensaje("Completa todos los campos para crear el usuario.", false);
            return;
        }
        if (contrasena.length() < LARGO_MINIMO_CONTRASENA) {
            mostrarMensaje("La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.", false);
            return;
        }
        if (usuarioDAO.existeNombreUsuario(nombreUsuario)) {
            mostrarMensaje("Ya existe un usuario con el nombre \"" + nombreUsuario + "\".", false);
            return;
        }

        String rol = ROL_TEXTO_ADMIN.equals(rolSeleccionado) ? Usuario.ROL_ADMINISTRADOR : Usuario.ROL_PROFESOR;
        try {
            usuarioDAO.crearUsuario(nombreCompleto, nombreUsuario, contrasena, rol);
            registrarEvento("usuario_creado", "Creó el usuario " + nombreUsuario + " (" + rolSeleccionado + ")");
            mostrarMensaje("Usuario \"" + nombreUsuario + "\" creado como " + rolSeleccionado + ".", true);
            campoNombreCompleto.clear();
            campoNombreUsuario.clear();
            campoContrasena.clear();
            comboRol.setValue(ROL_TEXTO_PROFESOR);
            cargarTabla();
        } catch (SQLException e) {
            mostrarMensaje("Error al crear el usuario: " + e.getMessage(), false);
        }
    }

    private TableCell<Usuario, Void> crearCeldaAcciones() {
        return new TableCell<Usuario, Void>() {
            private final Button botonRol = new Button("Cambiar Rol");
            private final Button botonClave = new Button("Restablecer Clave");
            private final Button botonEliminar = new Button("Eliminar");
            private final HBox contenedor = new HBox(6, botonRol, botonClave, botonEliminar);

            {
                botonRol.getStyleClass().add("boton-tabla");
                botonClave.getStyleClass().add("boton-tabla");
                botonEliminar.getStyleClass().add("boton-tabla-peligro");
                botonRol.setOnAction(e -> cambiarRol(getTableView().getItems().get(getIndex())));
                botonClave.setOnAction(e -> restablecerContrasena(getTableView().getItems().get(getIndex())));
                botonEliminar.setOnAction(e -> confirmarYEliminar(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Usuario fila = getTableView().getItems().get(getIndex());
                    boolean esMismoUsuario = esUsuarioActual(fila);
                    // Para no quedarse sin acceso por accidente: nadie puede
                    // quitarse su propio rol de administrador ni borrar su propia cuenta.
                    botonRol.setDisable(esMismoUsuario);
                    botonEliminar.setDisable(esMismoUsuario);
                    setGraphic(contenedor);
                }
            }
        };
    }

    private boolean esUsuarioActual(Usuario usuario) {
        Usuario actual = SesionActual.obtenerUsuario();
        return actual != null && actual.getId() == usuario.getId();
    }

    private void cambiarRol(Usuario usuario) {
        if (!SesionActual.esAdministrador() || esUsuarioActual(usuario)) {
            return;
        }
        String nuevoRol = usuario.esAdministrador() ? Usuario.ROL_PROFESOR : Usuario.ROL_ADMINISTRADOR;
        String textoNuevoRol = Usuario.ROL_ADMINISTRADOR.equals(nuevoRol) ? ROL_TEXTO_ADMIN : ROL_TEXTO_PROFESOR;
        try {
            usuarioDAO.cambiarRol(usuario.getId(), nuevoRol);
            registrarEvento("usuario_rol_cambiado", usuario.getNombreUsuario() + " ahora es " + textoNuevoRol);
            mostrarMensaje("\"" + usuario.getNombreUsuario() + "\" ahora es " + textoNuevoRol + ".", true);
            cargarTabla();
        } catch (SQLException e) {
            mostrarMensaje("Error al cambiar el rol: " + e.getMessage(), false);
        }
    }

    private void restablecerContrasena(Usuario usuario) {
        if (!SesionActual.esAdministrador()) {
            return;
        }
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Restablecer contraseña");
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField campoNueva = new PasswordField();
        campoNueva.setPromptText("Nueva contraseña (mínimo " + LARGO_MINIMO_CONTRASENA + " caracteres)");
        VBox contenido = new VBox(8,
                new Label("Nueva contraseña para \"" + usuario.getNombreUsuario() + "\":"), campoNueva);
        contenido.setPadding(new Insets(10));
        dialogo.getDialogPane().setContent(contenido);

        dialogo.showAndWait().ifPresent(resultado -> {
            if (resultado == ButtonType.OK) {
                String nueva = campoNueva.getText();
                if (nueva.length() < LARGO_MINIMO_CONTRASENA) {
                    mostrarMensaje("La contraseña debe tener al menos " + LARGO_MINIMO_CONTRASENA + " caracteres.", false);
                    return;
                }
                try {
                    usuarioDAO.restablecerContrasena(usuario.getId(), nueva);
                    registrarEvento("usuario_clave_restablecida", "Restableció la contraseña de " + usuario.getNombreUsuario());
                    mostrarMensaje("Contraseña de \"" + usuario.getNombreUsuario() + "\" restablecida.", true);
                } catch (SQLException e) {
                    mostrarMensaje("Error al restablecer la contraseña: " + e.getMessage(), false);
                }
            }
        });
    }

    private void confirmarYEliminar(Usuario usuario) {
        if (!SesionActual.esAdministrador() || esUsuarioActual(usuario)) {
            return;
        }
        int cursosDelUsuario = cursoDAO.contarPorDueno(usuario.getId());
        String aviso = cursosDelUsuario > 0
                ? "\n\nSus " + cursosDelUsuario + " curso(s) NO se borran: quedarán \"sin dueño\" "
                        + "hasta que los asignes a otro usuario."
                : "";
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Seguro que quieres eliminar al usuario \"" + usuario.getNombreUsuario() + "\"?" + aviso,
                ButtonType.YES, ButtonType.NO);
        confirmacion.setHeaderText(null);
        confirmacion.setTitle("Confirmar eliminación");

        confirmacion.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.YES) {
                try {
                    usuarioDAO.eliminarUsuario(usuario.getId());
                    registrarEvento("usuario_eliminado", "Eliminó al usuario " + usuario.getNombreUsuario());
                    mostrarMensaje("Usuario \"" + usuario.getNombreUsuario() + "\" eliminado.", true);
                    cargarTabla();
                } catch (SQLException e) {
                    mostrarMensaje("Error al eliminar el usuario: " + e.getMessage(), false);
                }
            }
        });
    }

    private void registrarEvento(String accion, String detalle) {
        Usuario actual = SesionActual.obtenerUsuario();
        logAuditoriaDAO.registrarEvento(accion, detalle, actual == null ? "desconocido" : actual.getNombreUsuario());
    }

    private void mostrarMensaje(String mensaje, boolean exito) {
        etiquetaResultado.setText(mensaje);
        etiquetaResultado.getStyleClass().removeAll("mensaje-exito", "mensaje-error");
        etiquetaResultado.getStyleClass().add(exito ? "mensaje-exito" : "mensaje-error");
    }
}