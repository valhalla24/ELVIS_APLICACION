package co.edu.usc.elvis.controller;

import co.edu.usc.elvis.dao.UsuarioDAO;
import co.edu.usc.elvis.model.Usuario;
import co.edu.usc.elvis.util.Mensajes;
import co.edu.usc.elvis.util.SeguridadUtil;
import co.edu.usc.elvis.util.SesionActual;
import java.io.IOException;
import java.net.URL;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField campoUsuario;
    @FXML private PasswordField campoContrasena;
    @FXML private TextField campoContrasenaVisible;
    @FXML private CheckBox checkMostrarContrasena;
    @FXML private ImageView imagenLogo;
    @FXML private Label textoLogo;
    @FXML private Label etiquetaError;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

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
        if (usuario == null) {
            etiquetaError.setText("Usuario o contraseña incorrectos.");
            return;
        }

        String hashIngresado = SeguridadUtil.hashContrasena(contrasena, usuario.getSal());
        if (!hashIngresado.equals(usuario.getContrasenaHash())) {
            etiquetaError.setText("Usuario o contraseña incorrectos.");
            return;
        }

        SesionActual.iniciarSesion(usuario);

        try {
            Parent raiz = FXMLLoader.load(getClass().getResource("/co/edu/usc/elvis/view/Principal.fxml"));
            Stage stage = (Stage) campoUsuario.getScene().getWindow();
            Scene escena = new Scene(raiz, 1200, 750);
            escena.getStylesheets().add(getClass().getResource("/co/edu/usc/elvis/view/estilos.css").toExternalForm());
            stage.setScene(escena);
        } catch (IOException e) {
            etiquetaError.setText("Error al cargar la aplicación: " + e.getMessage());
        }
    }
}