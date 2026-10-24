package co.edu.usc.elvis.controller;

import co.edu.usc.elvis.dao.UsuarioDAO;
import co.edu.usc.elvis.model.Usuario;
import co.edu.usc.elvis.util.SeguridadUtil;
import co.edu.usc.elvis.util.SesionActual;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField campoUsuario;
    @FXML private PasswordField campoContrasena;
    @FXML private Label etiquetaError;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    private void iniciarSesion() {
        String nombreUsuario = campoUsuario.getText().trim();
        String contrasena = campoContrasena.getText();

        if (nombreUsuario.isEmpty() || contrasena.isEmpty()) {
            etiquetaError.setText("Ingresa usuario y contraseña.");
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