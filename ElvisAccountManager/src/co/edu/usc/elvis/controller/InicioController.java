package co.edu.usc.elvis.controller;

import co.edu.usc.elvis.dao.EstudianteDAO;
import co.edu.usc.elvis.dao.LicenciaDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class InicioController {

    @FXML
    private Label etiquetaCapacidadTotal;
    @FXML
    private Label etiquetaLicenciasAsignadas;
    @FXML
    private Label etiquetaLicenciasDisponibles;
    @FXML
    private Label etiquetaPersonasRegistradas;

    private final LicenciaDAO licenciaDAO = new LicenciaDAO();
    private final EstudianteDAO estudianteDAO = new EstudianteDAO();

    @FXML
    public void initialize() {
        actualizarEstadisticas();
    }

    private void actualizarEstadisticas() {
        int total = licenciaDAO.contarLicenciasTotales();
        int asignadas = licenciaDAO.contarLicenciasAsignadas();
        int disponibles = licenciaDAO.contarLicenciasDisponibles();
        int personasRegistradas = estudianteDAO.contarTodos();

        etiquetaCapacidadTotal.setText(total + " Licencias");
        etiquetaLicenciasAsignadas.setText(asignadas + " Activas");
        etiquetaLicenciasDisponibles.setText(disponibles + " Libres");
        etiquetaPersonasRegistradas.setText(String.valueOf(personasRegistradas));
    }
}