package co.edu.usc.elvis.controller;

import co.edu.usc.elvis.dao.AsignacionDAO;
import co.edu.usc.elvis.dao.CursoDAO;
import co.edu.usc.elvis.dao.LicenciaDAO;
import co.edu.usc.elvis.model.Asignacion;
import co.edu.usc.elvis.model.Curso;
import co.edu.usc.elvis.util.HorarioUtil;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class CalendarioController {

    @FXML private GridPane grillaCalendario;
    @FXML private Label etiquetaAlerta;
    @FXML private Label etiquetaPicoOcupacion;

    private final CursoDAO cursoDAO = new CursoDAO();
    private final AsignacionDAO asignacionDAO = new AsignacionDAO();
    private final LicenciaDAO licenciaDAO = new LicenciaDAO();

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
    private static final double UMBRAL_ALTA = 0.85;
    private static final double UMBRAL_MODERADA = 0.70;

    @FXML
    public void initialize() {
        construirGrilla();
    }

    private void construirGrilla() {
        grillaCalendario.getChildren().clear();

        List<Curso> cursos = cursoDAO.obtenerTodos();
        int totalLicencias = licenciaDAO.contarLicenciasTotales();
        List<String> horas = HorarioUtil.generarFranjasHorarias();

        Label esquina = new Label("Hora");
        esquina.getStyleClass().add("calendario-encabezado");
        grillaCalendario.add(esquina, 0, 0);
        for (int col = 0; col < DIAS.length; col++) {
            Label encabezadoDia = new Label(DIAS[col]);
            encabezadoDia.getStyleClass().add("calendario-encabezado");
            grillaCalendario.add(encabezadoDia, col + 1, 0);
        }

        double picoOcupacion = -1;
        String picoDia = "";
        String picoHora = "";
        int picoUso = 0;

        for (int fila = 0; fila < horas.size(); fila++) {
            String hora = horas.get(fila);
            Label etiquetaHora = new Label(hora);
            etiquetaHora.getStyleClass().add("calendario-hora");
            grillaCalendario.add(etiquetaHora, 0, fila + 1);

            for (int col = 0; col < DIAS.length; col++) {
                String dia = DIAS[col];
                List<Curso> cursosEnCelda = new ArrayList<>();
                for (Curso c : cursos) {
                    if (c.getDiaClase().equals(dia) && HorarioUtil.incluyeHora(c.getHoraInicio(), c.getHoraFin(), hora)) {
                        cursosEnCelda.add(c);
                    }
                }

                int enUso = contarLicenciasEnUso(dia, hora);
                double ocupacion = totalLicencias == 0 ? 0 : (double) enUso / totalLicencias;
                if (ocupacion > picoOcupacion) {
                    picoOcupacion = ocupacion;
                    picoDia = dia;
                    picoHora = hora;
                    picoUso = enUso;
                }

                VBox celda = construirCelda(cursosEnCelda, ocupacion);
                grillaCalendario.add(celda, col + 1, fila + 1);
            }
        }

        mostrarResumen(Math.max(picoOcupacion, 0), picoDia, picoHora, picoUso, totalLicencias);
    }

    private int contarLicenciasEnUso(String dia, String hora) {
        List<Asignacion> asignaciones = asignacionDAO.obtenerAsignacionesPorDia(dia);
        int contador = 0;
        for (Asignacion a : asignaciones) {
            if (HorarioUtil.incluyeHora(a.getHoraInicio(), a.getHoraFin(), hora)) {
                contador++;
            }
        }
        return contador;
    }

    private VBox construirCelda(List<Curso> cursosEnCelda, double ocupacion) {
        VBox celda = new VBox(2);
        celda.getStyleClass().add("calendario-celda");
        if (ocupacion > UMBRAL_ALTA) {
            celda.getStyleClass().add("celda-alta");
        } else if (ocupacion >= UMBRAL_MODERADA) {
            celda.getStyleClass().add("celda-moderada");
        } else if (ocupacion > 0) {
            celda.getStyleClass().add("celda-normal");
        }

        for (Curso c : cursosEnCelda) {
            Label etiquetaCurso = new Label(c.getNombreMateria() + " · " + c.getLicenciasRequeridas() + " lic.");
            etiquetaCurso.getStyleClass().add("calendario-curso");
            etiquetaCurso.setWrapText(true);
            celda.getChildren().add(etiquetaCurso);
        }
        return celda;
    }

    private void mostrarResumen(double picoOcupacion, String picoDia, String picoHora, int picoUso, int total) {
        if (picoOcupacion > UMBRAL_ALTA) {
            etiquetaAlerta.setText("Alerta de Cupo de Licencias: " + picoDia + " en la franja " + picoHora
                    + " se alcanza " + picoUso + "/" + total + " (" + String.format("%.1f", picoOcupacion * 100)
                    + "% de asignación). Se recomienda reasignar cupos de cursos en otros horarios.");
            etiquetaAlerta.setVisible(true);
            etiquetaAlerta.setManaged(true);
        } else {
            etiquetaAlerta.setText("");
            etiquetaAlerta.setVisible(false);
            etiquetaAlerta.setManaged(false);
        }

        if (total == 0) {
            etiquetaPicoOcupacion.setText("Aún no hay licencias cargadas en el sistema.");
        } else if (picoUso == 0) {
            etiquetaPicoOcupacion.setText("Todavía no hay licencias asignadas a ningún curso.");
        } else {
            etiquetaPicoOcupacion.setText("Pico máximo de cuota: " + String.format("%.1f", picoOcupacion * 100)
                    + "% (" + picoUso + "/" + total + " licencias) — " + picoDia + " " + picoHora);
        }
    }
}