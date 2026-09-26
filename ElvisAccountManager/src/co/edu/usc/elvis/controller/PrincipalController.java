package co.edu.usc.elvis.controller;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class PrincipalController {

    @FXML private StackPane contenedorVistas;
    @FXML private Button botonInicio;
    @FXML private Button botonCuentas;
    @FXML private Button botonCalendario;

    @FXML
    public void initialize() {
        mostrarInicio();
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
        if (!botonActivo.getStyleClass().contains("boton-nav-activo")) {
            botonActivo.getStyleClass().add("boton-nav-activo");
        }
    }
}