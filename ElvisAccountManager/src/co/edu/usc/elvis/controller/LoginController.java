package co.edu.usc.elvis.controller;

import co.edu.usc.elvis.dao.LogAuditoriaDAO;
import co.edu.usc.elvis.dao.UsuarioDAO;
import co.edu.usc.elvis.model.Usuario;
import co.edu.usc.elvis.util.Mensajes;
import co.edu.usc.elvis.util.SeguridadUtil;
import co.edu.usc.elvis.util.SesionActual;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public class LoginController {

    private static final String RUTA_ESTILOS = "/co/edu/usc/elvis/view/estilos.css";

    @FXML private TextField campoUsuario;
    @FXML private PasswordField campoContrasena;
    @FXML private TextField campoContrasenaVisible;
    @FXML private CheckBox checkMostrarContrasena;
    @FXML private ImageView imagenLogo;
    @FXML private Label textoLogo;
    @FXML private Label etiquetaError;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final LogAuditoriaDAO logAuditoriaDAO = new LogAuditoriaDAO();

    @FXML
    public void initialize() {
        // El campo de texto visible y el de contraseña comparten el mismo texto:
        // "Mostrar contraseña" solo cambia cual de los dos se ve.
        campoContrasenaVisible.textProperty().bindBidirectional(campoContrasena.textProperty());
        campoContrasenaVisible.visibleProperty().bind(checkMostrarContrasena.selectedProperty());
        campoContrasenaVisible.managedProperty().bind(checkMostrarContrasena.selectedProperty());
        campoContrasena.visibleProperty().bind(checkMostrarContrasena.selectedProperty().not());
        campoContrasena.managedProperty().bind(checkMostrarContrasena.selectedProperty().not());

        Mensajes.configurar(etiquetaError);
        cargarLogo();
        Platform.runLater(campoUsuario::requestFocus);
    }

    /** Si existe view/logo.png se muestra el logo; si no, queda el escudo de texto. */
    private void cargarLogo() {
        URL recurso = getClass().getResource("/co/edu/usc/elvis/view/logo.png");
        boolean hayLogo = recurso != null;
        if (hayLogo) {
            imagenLogo.setImage(new Image(recurso.toExternalForm()));
        }
        imagenLogo.setVisible(hayLogo);
        imagenLogo.setManaged(hayLogo);
        textoLogo.setVisible(!hayLogo);
        textoLogo.setManaged(!hayLogo);
    }

    @FXML
    private void irAContrasena() {
        enfocarContrasena();
    }

    @FXML
    private void alternarContrasena() {
        enfocarContrasena();
    }

    private void enfocarContrasena() {
        TextField activo = checkMostrarContrasena.isSelected() ? campoContrasenaVisible : campoContrasena;
        Platform.runLater(() -> {
            activo.requestFocus();
            activo.positionCaret(activo.getText().length());
        });
    }

    @FXML
    private void iniciarSesion() {
        String nombreUsuario = campoUsuario.getText().trim();
        String contrasena = campoContrasena.getText();

        if (nombreUsuario.isEmpty() || contrasena.isEmpty()) {
            etiquetaError.setText("Escribe tu usuario y tu contraseña.");
            return;
        }

        Usuario usuario = usuarioDAO.buscarPorNombreUsuario(nombreUsuario);
        if (usuario == null || !SeguridadUtil.verificar(contrasena, usuario.getSal(), usuario.getContrasenaHash())) {
            etiquetaError.setText("Usuario o contraseña incorrectos.");
            return;
        }

        try {
            // Cuentas de versiones anteriores (SHA-256): se re-guardan con PBKDF2 ahora que se conoce la clave.
            if (SeguridadUtil.necesitaActualizar(usuario.getContrasenaHash())) {
                usuarioDAO.actualizarHash(usuario.getId(), contrasena);
            }
            // Cuenta nueva, clave restablecida o la clave inicial del sistema: no se entra sin cambiarla.
            if (usuario.isDebeCambiarClave() || SeguridadUtil.CLAVE_INICIAL_ADMIN.equals(contrasena)) {
                String nueva = pedirNuevaContrasena(contrasena);
                if (nueva == null) {
                    etiquetaError.setText("Debes cambiar la contraseña para entrar.");
                    return;
                }
                usuarioDAO.cambiarContrasenaPropia(usuario.getId(), nueva);
                logAuditoriaDAO.registrarEvento("usuario_clave_cambiada",
                        "Cambió su contraseña al iniciar sesión", usuario.getNombreUsuario());
            }
        } catch (SQLException e) {
            etiquetaError.setText("No se pudo guardar la contraseña: " + e.getMessage());
            return;
        }

        SesionActual.iniciarSesion(usuario);

        try {
            Parent raiz = FXMLLoader.load(getClass().getResource("/co/edu/usc/elvis/view/Principal.fxml"));
            // Se cambia solo el contenido de la ventana (NO se crea una escena nueva):
            // asi la ventana conserva su tamaño y su estado de pantalla completa.
            Scene escena = campoUsuario.getScene();
            String estilos = getClass().getResource(RUTA_ESTILOS).toExternalForm();
            if (!escena.getStylesheets().contains(estilos)) {
                escena.getStylesheets().add(estilos);
            }
            escena.setRoot(raiz);
        } catch (IOException e) {
            etiquetaError.setText("Error al cargar la aplicación: " + e.getMessage());
        }
    }

    /** Pide la contraseña nueva dos veces; devuelve null si el usuario cancela. */
    private String pedirNuevaContrasena(String actual) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Cambiar contraseña");
        dialogo.setHeaderText("Por seguridad, elige una contraseña nueva antes de continuar.");
        if (campoUsuario.getScene() != null) {
            dialogo.initOwner(campoUsuario.getScene().getWindow());
        }
        PasswordField nueva = new PasswordField();
        PasswordField confirmacion = new PasswordField();
        Label error = new Label();
        error.getStyleClass().add("mensaje-error");
        error.setWrapText(true);
        GridPane grilla = new GridPane();
        grilla.setHgap(10);
        grilla.setVgap(10);
        grilla.addRow(0, new Label("Nueva contraseña (mínimo " + SeguridadUtil.LARGO_MINIMO + " caracteres):"), nueva);
        grilla.addRow(1, new Label("Repítela:"), confirmacion);
        grilla.add(error, 0, 2, 2, 1);
        dialogo.getDialogPane().setContent(grilla);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogo.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, evento -> {
            String motivo = SeguridadUtil.validarNuevaContrasena(nueva.getText(), confirmacion.getText(), actual);
            if (motivo != null) {
                error.setText(motivo);
                evento.consume();   // el dialogo sigue abierto hasta que la clave sea valida
            }
        });
        Platform.runLater(nueva::requestFocus);
        return dialogo.showAndWait().filter(b -> b == ButtonType.OK).map(b -> nueva.getText()).orElse(null);
    }
}