package co.edu.usc.elvis.controller;

import co.edu.usc.elvis.dao.AsignacionDAO;
import co.edu.usc.elvis.dao.CursoDAO;
import co.edu.usc.elvis.dao.LicenciaDAO;
import co.edu.usc.elvis.model.Asignacion;
import co.edu.usc.elvis.model.Curso;
import co.edu.usc.elvis.util.HorarioUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Calendario semanal en franjas de media hora. Hace solo DOS consultas a la
 * base (cursos y asignaciones) y el resto es calculo en memoria.
 */
public class CalendarioController {

    @FXML private GridPane grillaCalendario;
    @FXML private Label etiquetaAlerta;
    @FXML private Label etiquetaPicoOcupacion;

    private final CursoDAO cursoDAO = new CursoDAO();
    private final AsignacionDAO asignacionDAO = new AsignacionDAO();
    private final LicenciaDAO licenciaDAO = new LicenciaDAO();

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
    private static final int MINUTOS_POR_FRANJA = 30;
    private static final double UMBRAL_ALTA = 0.85;
    private static final double UMBRAL_MODERADA = 0.70;

    /** Un curso con su horario ya convertido a minutos, para no repetir la conversion en cada casilla. */
    private static class CursoEnMinutos {
        final Curso curso;
        final int inicio;
        final int fin;

        CursoEnMinutos(Curso curso) {
            this.curso = curso;
            this.inicio = HorarioUtil.aMinutos(curso.getHoraInicio());
            this.fin = HorarioUtil.aMinutos(curso.getHoraFin());
        }
    }

    @FXML
    public void initialize() {
        construirGrilla();
    }

    private void construirGrilla() {
        grillaCalendario.getChildren().clear();

        int totalLicencias = licenciaDAO.contarLicenciasTotales();
        List<String> horas = HorarioUtil.generarFranjasHorarias();

        // Consulta 1: cursos agrupados por dia
        Map<String, List<CursoEnMinutos>> cursosPorDia = new HashMap<>();
        for (Curso c : cursoDAO.obtenerTodos()) {
            cursosPorDia.computeIfAbsent(c.getDiaClase(), k -> new ArrayList<>()).add(new CursoEnMinutos(c));
        }
        // Consulta 2: asignaciones agrupadas por dia, como [inicio, fin] en minutos
        Map<String, List<int[]>> usoPorDia = new HashMap<>();
        for (Asignacion a : asignacionDAO.obtenerTodas()) {
            usoPorDia.computeIfAbsent(a.getDiaFranja(), k -> new ArrayList<>())
                    .add(new int[]{HorarioUtil.aMinutos(a.getHoraInicio()), HorarioUtil.aMinutos(a.getHoraFin())});
        }

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
            int minutosFranja = HorarioUtil.aMinutos(hora);

            Label etiquetaHora = new Label(hora);
            etiquetaHora.getStyleClass().add("calendario-hora");
            grillaCalendario.add(etiquetaHora, 0, fila + 1);

            for (int col = 0; col < DIAS.length; col++) {
                String dia = DIAS[col];

                // cursos que ocupan esta franja; la etiqueta solo se escribe en la primera franja del curso
                List<CursoEnMinutos> cursosQueEmpiezanAqui = new ArrayList<>();
                for (CursoEnMinutos c : cursosPorDia.getOrDefault(dia, new ArrayList<>())) {
                    boolean cubreLaFranja = minutosFranja >= c.inicio && minutosFranja < c.fin;
                    boolean esLaPrimera = c.inicio > minutosFranja - MINUTOS_POR_FRANJA;
                    if (cubreLaFranja && esLaPrimera) {
                        cursosQueEmpiezanAqui.add(c);
                    }
                }

                int enUso = 0;
                for (int[] rango : usoPorDia.getOrDefault(dia, new ArrayList<>())) {
                    if (minutosFranja >= rango[0] && minutosFranja < rango[1]) {
                        enUso++;
                    }
                }
                double ocupacion = totalLicencias == 0 ? 0 : (double) enUso / totalLicencias;
                if (ocupacion > picoOcupacion) {
                    picoOcupacion = ocupacion;
                    picoDia = dia;
                    picoHora = hora;
                    picoUso = enUso;
                }

                VBox celda = construirCelda(cursosQueEmpiezanAqui, ocupacion);
                grillaCalendario.add(celda, col + 1, fila + 1);
            }
        }

        mostrarResumen(Math.max(picoOcupacion, 0), picoDia, picoHora, picoUso, totalLicencias);
    }

    private VBox construirCelda(List<CursoEnMinutos> cursosQueEmpiezanAqui, double ocupacion) {
        VBox celda = new VBox(1);
        celda.getStyleClass().add("calendario-celda");
        if (ocupacion > UMBRAL_ALTA) {
            celda.getStyleClass().add("celda-alta");
        } else if (ocupacion >= UMBRAL_MODERADA) {
            celda.getStyleClass().add("celda-moderada");
        } else if (ocupacion > 0) {
            celda.getStyleClass().add("celda-normal");
        }

        for (CursoEnMinutos c : cursosQueEmpiezanAqui) {
            Label nombre = new Label(c.curso.getNombreMateria() + " · " + c.curso.getLicenciasRequeridas() + " lic.");
            nombre.getStyleClass().add("calendario-curso");
            nombre.setWrapText(true);
            Label horario = new Label(c.curso.getHoraInicio() + " - " + c.curso.getHoraFin());
            horario.getStyleClass().add("calendario-curso-horario");
            celda.getChildren().addAll(nombre, horario);
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