package co.edu.usc.elvis.controller;

import co.edu.usc.elvis.model.Usuario;
import co.edu.usc.elvis.util.SesionActual;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class PrincipalController {

    @FXML private StackPane contenedorVistas;
    @FXML private Button botonInicio;
    @FXML private Button botonCuentas;
    @FXML private Button botonCalendario;
    @FXML private Button botonReportes;
    @FXML private Button botonUsuarios;
    @FXML private MenuButton menuPerfil;
    @FXML private Label avatarPerfil;
    @FXML private Label etiquetaNombrePerfil;
    @FXML private Label etiquetaRolPerfil;
    @FXML private MenuItem itemPerfilDatos;

    @FXML
    public void initialize() {
        Usuario usuario = SesionActual.obtenerUsuario();
        if (usuario != null) {
            String rolLegible = usuario.esAdministrador() ? "Administrador" : "Profesor";
            avatarPerfil.setText(iniciales(usuario.getNombreCompleto()));
            etiquetaNombrePerfil.setText(usuario.getNombreCompleto());
            etiquetaRolPerfil.setText(rolLegible);
            itemPerfilDatos.setText("Sesión iniciada como\n" + usuario.getNombreUsuario() + " · " + rolLegible);
        }

        boolean esAdmin = SesionActual.esAdministrador();
        botonUsuarios.setVisible(esAdmin);
        botonUsuarios.setManaged(esAdmin);

        mostrarInicio();
    }

    /** Primera letra del primer nombre y del ultimo apellido: "Ana Gómez" -> "AG". */
    static String iniciales(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
            return "?";
        }
        String[] partes = nombreCompleto.trim().split("\\s+");
        String inicial = partes[0].substring(0, 1);
        if (partes.length > 1) {
            inicial += partes[partes.length - 1].substring(0, 1);
        }
        return inicial.toUpperCase();
    }

    @FXML
    private void mostrarInicio() {
        cargarVista("/co/edu/usc/elvis/view/Inicio.fxml");
        marcarActivo(botonInicio);
    }

    @FXML
    private void mostrarAdministrarCuentas() {
        cargarVista("/co/edu/usc/elvis/view/AdministrarCuentas.fxml");
        marcarActivo(botonCuentas);
    }

    @FXML
    private void mostrarCalendario() {
        cargarVista("/co/edu/usc/elvis/view/Calendario.fxml");
        marcarActivo(botonCalendario);
    }

    @FXML
    private void mostrarReportes() {
        cargarVista("/co/edu/usc/elvis/view/Reportes.fxml");
        marcarActivo(botonReportes);
    }

    @FXML
    private void mostrarUsuarios() {
        if (!SesionActual.esAdministrador()) {
            return;
        }
        cargarVista("/co/edu/usc/elvis/view/Usuarios.fxml");
        marcarActivo(botonUsuarios);
    }

    @FXML
    private void cerrarSesion() {
        SesionActual.cerrarSesion();
        try {
            Parent raiz = FXMLLoader.load(getClass().getResource("/co/edu/usc/elvis/view/Login.fxml"));
            Stage stage = (Stage) contenedorVistas.getScene().getWindow();
            Scene escena = new Scene(raiz, 1200, 750);
            escena.getStylesheets().add(getClass().getResource("/co/edu/usc/elvis/view/estilos.css").toExternalForm());
            stage.setScene(escena);
        } catch (IOException e) {
            System.err.println("No se pudo volver al login: " + e.getMessage());
        }
    }

    private void cargarVista(String rutaFxml) {
        try {
            Parent vista = FXMLLoader.load(getClass().getResource(rutaFxml));
            contenedorVistas.getChildren().setAll(vista);
        } catch (IOException e) {
            System.err.println("No se pudo cargar la vista: " + rutaFxml);
            e.printStackTrace();
        }
    }

    private void marcarActivo(Button botonActivo) {
        botonInicio.getStyleClass().remove("boton-nav-activo");
        botonCuentas.getStyleClass().remove("boton-nav-activo");
        botonCalendario.getStyleClass().remove("boton-nav-activo");
        botonReportes.getStyleClass().remove("boton-nav-activo");
        botonUsuarios.getStyleClass().remove("boton-nav-activo");
        if (!botonActivo.getStyleClass().contains("boton-nav-activo")) {
            botonActivo.getStyleClass().add("boton-nav-activo");
        }
    }
}