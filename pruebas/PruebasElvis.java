import co.edu.usc.elvis.dao.AsignacionDAO;
import co.edu.usc.elvis.dao.ConexionBD;
import co.edu.usc.elvis.dao.CursoDAO;
import co.edu.usc.elvis.dao.EstudianteDAO;
import co.edu.usc.elvis.dao.LicenciaDAO;
import co.edu.usc.elvis.dao.UsuarioDAO;
import co.edu.usc.elvis.model.AsignacionDetalle;
import co.edu.usc.elvis.model.Curso;
import co.edu.usc.elvis.model.Estudiante;
import co.edu.usc.elvis.model.Licencia;
import co.edu.usc.elvis.model.ReporteDatos;
import co.edu.usc.elvis.model.Usuario;
import co.edu.usc.elvis.util.ExportadorExcel;
import co.edu.usc.elvis.util.ExportadorPdf;
import co.edu.usc.elvis.util.ReporteService;
import co.edu.usc.elvis.util.SesionActual;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Pruebas de calendario, reportes y restriccion por rol sobre las pantallas,
 * servicios y DAO reales, con una base temporal (-Delvis.datos=...).
 */
public class PruebasElvis {

    static int ok = 0, falla = 0;
    static final List<String> filas = new ArrayList<>();

    static void caso(String id, String desc, boolean cumple, String detalle) {
        if (cumple) ok++; else falla++;
        String linea = String.format("%-7s %-6s %s%s", id, cumple ? "OK" : "FALLA", desc,
                cumple || detalle == null ? "" : "  -> " + detalle);
        filas.add(linea);
        System.out.println(linea);
    }

    static <T> T fx(Callable<T> c) throws Exception {
        CompletableFuture<T> f = new CompletableFuture<>();
        Platform.runLater(() -> { try { f.complete(c.call()); } catch (Throwable t) { f.completeExceptionally(t); } });
        return f.get();
    }

    static Object campo(Object obj, String nombre) throws Exception {
        Field f = obj.getClass().getDeclaredField(nombre);
        f.setAccessible(true);
        return f.get(obj);
    }

    static void invocar(Object obj, String metodo) throws Exception {
        Method m = obj.getClass().getDeclaredMethod(metodo);
        m.setAccessible(true);
        m.invoke(obj);
    }

    static Usuario usuario(String nombre) { return new UsuarioDAO().buscarPorNombreUsuario(nombre); }

    static Curso curso(String codigo) {
        return new CursoDAO().obtenerTodos().stream().filter(c -> c.getCodigoCurso().equals(codigo)).findFirst().orElse(null);
    }

    static Curso nuevoCurso(String nombre, String codigo, String dia, String hi, String hf, int lic, int duenoId) throws Exception {
        Curso c = new Curso(nombre, codigo, "2026-B", "Docente " + codigo, dia, hi, hf, lic);
        c.setUsuarioId(duenoId);
        new CursoDAO().registrarCurso(c);
        Curso guardado = curso(codigo);
        EstudianteDAO est = new EstudianteDAO();
        for (int i = 1; i <= lic; i++) {
            String cod = codigo + "-E" + i;
            est.insertarEstudiante(new Estudiante("Estudiante " + cod, cod, cod.toLowerCase() + "@prueba.invalid", guardado.getId()));
        }
        return guardado;
    }

    /** Asigna en orden las licencias indicadas a los estudiantes del curso (como lo hace la pantalla). */
    static void asignar(Curso c, List<Licencia> lics) throws Exception {
        AsignacionDAO a = new AsignacionDAO();
        LicenciaDAO l = new LicenciaDAO();
        List<Estudiante> ests = new EstudianteDAO().obtenerPorCurso(c.getId());
        for (int i = 0; i < lics.size(); i++) {
            a.crearAsignacion(lics.get(i).getId(), c.getId(), ests.get(i).getId(), c.getDiaClase(), c.getHoraInicio(), c.getHoraFin());
            l.recalcularEstado(lics.get(i).getId(), a.contarAsignacionesPorLicencia(lics.get(i).getId()));
        }
    }

    static VBox celda(GridPane g, int col, int fila) {
        for (Node n : g.getChildren()) {
            Integer c = GridPane.getColumnIndex(n), f = GridPane.getRowIndex(n);
            if (c != null && f != null && c == col && f == fila && n instanceof VBox) return (VBox) n;
        }
        return null;
    }

    static String textoCelda(VBox v) {
        return v.getChildren().stream().filter(n -> n instanceof Label).map(n -> ((Label) n).getText()).collect(Collectors.joining(" | "));
    }

    static String nivel(VBox v) {
        for (String s : v.getStyleClass()) if (s.startsWith("celda-")) return s;
        return "vacía";
    }

    public static void main(String[] args) throws Exception {
        File salida = new File(args[0]);
        salida.mkdirs();
        ConexionBD.inicializarBaseDeDatos();
        UsuarioDAO udao = new UsuarioDAO();
        udao.crearUsuario("Profesora Ana", "profa", "clave-a1", Usuario.ROL_PROFESOR);
        udao.crearUsuario("Profesor Beto", "profb", "clave-b1", Usuario.ROL_PROFESOR);
        Usuario admin = usuario("admin"), profA = usuario("profa"), profB = usuario("profb");

        LicenciaDAO ldao = new LicenciaDAO();
        for (int i = 1; i <= 10; i++) ldao.insertarLicencia(String.format("lic%02d", i), String.format("Clave-%02d", i), "10.0.0.1");
        List<Licencia> L = ldao.obtenerTodas();   // ordenadas por usuario: lic01..lic10

        // Escenario: capacidad 10. Lunes 9:00 -> 9/10 (alta), Miercoles 8:00 -> 7/10 (moderada).
        Curso a1 = nuevoCurso("Curso A1", "A1", "Lunes", "08:00 a.m.", "10:00 a.m.", 6, profA.getId());
        Curso a2 = nuevoCurso("Curso A2", "A2", "Miércoles", "08:00 a.m.", "08:30 a.m.", 7, profA.getId());
        Curso b1 = nuevoCurso("Curso B1", "B1", "Lunes", "09:00 a.m.", "11:00 a.m.", 3, profB.getId());
        Curso b2 = nuevoCurso("Curso B2", "B2", "Martes", "08:00 a.m.", "09:00 a.m.", 2, profB.getId());
        Curso b3 = nuevoCurso("Curso B3", "B3", "Jueves", "02:00 p.m.", "03:00 p.m.", 1, profB.getId());
        asignar(a1, L.subList(0, 6));
        asignar(b1, L.subList(6, 9));
        asignar(b2, L.subList(0, 2));
        asignar(a2, L.subList(0, 7));

        Platform.startup(() -> { });

        // ------------------------------------------------------------ CALENDARIO
        SesionActual.iniciarSesion(profA);
        FXMLLoader cal = new FXMLLoader(PruebasElvis.class.getResource("/co/edu/usc/elvis/view/Calendario.fxml"));
        fx(() -> cal.load());
        Object calCtrl = cal.getController();
        GridPane g = (GridPane) campo(calCtrl, "grillaCalendario");
        List<String> horas = co.edu.usc.elvis.util.HorarioUtil.generarFranjasHorarias();
        java.util.function.BiFunction<Integer, String, VBox> en = (dia, hora) -> celda(g, dia, horas.indexOf(hora) + 1);
        // columnas: 1 Lunes, 2 Martes, 3 Miercoles, 4 Jueves
        VBox c;
        c = en.apply(1, "08:00 a.m.");
        caso("CAL-01", "Lunes 08:00: 6/10 = 60 % -> normal y muestra el curso que empieza", nivel(c).equals("celda-normal") && textoCelda(c).contains("Curso A1 · 6 lic."), nivel(c) + " / " + textoCelda(c));
        c = en.apply(1, "08:30 a.m.");
        caso("CAL-02", "Lunes 08:30: el curso no se repite en la franja siguiente", nivel(c).equals("celda-normal") && !textoCelda(c).contains("Curso A1"), textoCelda(c));
        c = en.apply(1, "09:00 a.m.");
        caso("CAL-03", "Lunes 09:00: 9/10 = 90 % -> alta (dos cursos solapados)", nivel(c).equals("celda-alta") && textoCelda(c).contains("Curso B1"), nivel(c) + " / " + textoCelda(c));
        c = en.apply(1, "10:00 a.m.");
        caso("CAL-04", "Lunes 10:00: A1 termina (fin exclusivo), quedan 3/10 -> normal", nivel(c).equals("celda-normal"), nivel(c));
        c = en.apply(3, "08:00 a.m.");
        caso("CAL-05", "Miércoles 08:00: 7/10 = 70 % -> moderada (límite inferior)", nivel(c).equals("celda-moderada"), nivel(c));
        c = en.apply(1, "11:00 a.m.");
        caso("CAL-06", "Lunes 11:00: sin uso -> sin color", nivel(c).equals("vacía"), nivel(c));
        c = en.apply(2, "08:00 a.m.");
        caso("CAL-07", "Martes 08:00: licencias de otro día no cuentan (2/10)", nivel(c).equals("celda-normal"), nivel(c));
        Label alerta = (Label) campo(calCtrl, "etiquetaAlerta");
        caso("CAL-08", "Alerta visible con día, franja y uso del pico", alerta.isVisible() && alerta.getText().contains("Lunes") && alerta.getText().contains("09:00 a.m.") && alerta.getText().contains("9/10"), alerta.getText());
        Label pico = (Label) campo(calCtrl, "etiquetaPicoOcupacion");
        caso("CAL-09", "Resumen del pico: 90,0 % (9/10) Lunes 09:00", pico.getText().matches(".*90[,.]0% \\(9/10 licencias\\).*Lunes 09:00 a\\.m\\..*"), pico.getText());

        // ------------------------------------------------------------ REPORTES
        ReporteService rs = new ReporteService();
        ReporteDatos r = rs.ocupacionPorCurso(admin, null);
        caso("REP-01", "Ocupación por curso (admin): 5 cursos y columna Dueño", r.getFilas().size() == 5 && r.getColumnas().contains("Dueño"), r.getFilas().size() + " filas, columnas " + r.getColumnas());
        ReporteDatos ra = rs.ocupacionPorCurso(profA, null);
        List<Object> codigosA = ra.getFilas().stream().map(f -> f.get(0)).collect(Collectors.toList());
        caso("REP-02", "Ocupación por curso (profesor): solo sus cursos y sin columna Dueño", codigosA.equals(List.of("A1", "A2")) && !ra.getColumnas().contains("Dueño"), codigosA + " " + ra.getColumnas());
        double pctA1 = ((Number) r.getFilas().stream().filter(f -> "A1".equals(f.get(0))).findFirst().get().get(r.getColumnas().indexOf("% del cupo total"))).doubleValue();
        caso("REP-03", "Ocupación por curso: A1 usa 6 de 10 licencias = 60 %", Math.abs(pctA1 - 60.0) < 0.001, String.valueOf(pctA1));
        ReporteDatos rb = rs.asignaciones(profB, null, null, null);
        boolean soloB = rb.getFilas().stream().allMatch(f -> String.valueOf(f.get(0)).startsWith("B"));
        caso("REP-04", "Asignaciones (profesor B): 5 filas, todas de sus cursos", rb.getFilas().size() == 5 && soloB, rb.getFilas().size() + " filas");
        LocalDate hoy = LocalDate.now();
        int todas = rs.asignaciones(admin, null, null, null).getFilas().size();
        int rangoHoy = rs.asignaciones(admin, null, hoy, hoy).getFilas().size();
        int rangoFuturo = rs.asignaciones(admin, null, hoy.plusDays(1), null).getFilas().size();
        caso("REP-05", "Asignaciones con rango de fechas: hoy = todas (18), desde mañana = 0", todas == 18 && rangoHoy == 18 && rangoFuturo == 0, todas + "/" + rangoHoy + "/" + rangoFuturo);
        ReporteDatos rf = rs.ocupacionPorFranja(admin, null);
        String lun9 = rf.getFilas().stream().filter(f -> "Lunes".equals(f.get(0)) && "09:00 a.m.".equals(f.get(1))).map(f -> f.get(2) + " " + f.get(5)).findFirst().orElse("-");
        String mie8 = rf.getFilas().stream().filter(f -> "Miércoles".equals(f.get(0)) && "08:00 a.m.".equals(f.get(1))).map(f -> f.get(2) + " " + f.get(5)).findFirst().orElse("-");
        caso("REP-06", "Ocupación por franja: Lunes 09:00 = 9 Alta; Miércoles 08:00 = 7 Moderada", lun9.equals("9 Alta") && mie8.equals("7 Moderada"), lun9 + " / " + mie8);
        boolean prohibida = false;
        try { rs.bitacora(profA, null, null, null); } catch (IllegalArgumentException e) { prohibida = true; }
        ReporteDatos rbit = rs.bitacora(admin, null, null, null);
        caso("REP-07", "Bitácora: el profesor no puede generarla; el administrador sí", prohibida && rbit.getColumnas().size() == 4, "prohibida=" + prohibida);
        ReporteDatos rTodas = rs.asignaciones(admin, null, null, null);
        boolean sinClaves = rTodas.getFilas().stream().flatMap(List::stream).noneMatch(v -> String.valueOf(v).contains("Clave-"));
        caso("REP-08", "Ningún reporte incluye contraseñas de licencias", sinClaves, null);
        File pdf = new File(salida, "asignaciones.pdf"), xlsx = new File(salida, "asignaciones.xlsx");
        ExportadorPdf.exportar(rTodas, pdf);
        ExportadorExcel.exportar(rTodas, xlsx);
        byte[] bpdf = Files.readAllBytes(pdf.toPath());
        boolean pdfOk = new String(bpdf, 0, 5, "ISO-8859-1").equals("%PDF-") && !new String(bpdf, "ISO-8859-1").contains("Clave-");
        int filasExcel = 0; boolean excelSinClaves = true;
        try (XSSFWorkbook wb = new XSSFWorkbook(xlsx)) {
            Sheet s = wb.getSheetAt(0);
            for (Row row : s) for (Cell cell : row) {
                String t = cell.toString();
                if (t.contains("Clave-")) excelSinClaves = false;
                if (t.startsWith("estudiante ") || t.startsWith("Estudiante ")) { filasExcel++; break; }
            }
        }
        caso("REP-09", "Exportar a PDF y Excel: archivos válidos, 18 filas y sin contraseñas", pdfOk && excelSinClaves && filasExcel == 18, "pdf=" + pdfOk + " excel filas=" + filasExcel + " sinClaves=" + excelSinClaves);

        // ------------------------------------------------------------ RESTRICCION POR ROL
        CursoDAO cdao = new CursoDAO();
        AsignacionDAO adao = new AsignacionDAO();
        List<String> cursosA = cdao.obtenerPorDueno(profA.getId()).stream().map(Curso::getCodigoCurso).sorted().collect(Collectors.toList());
        caso("ROL-01", "DAO: el profesor A solo obtiene sus cursos", cursosA.equals(List.of("A1", "A2")), cursosA.toString());
        boolean detB = adao.obtenerConDetallePorDueno(profB.getId()).stream().allMatch(d -> d.getCursoCodigo().startsWith("B"));
        boolean pendA = adao.obtenerPendientesDeNotificarPorDueno(profA.getId()).stream().allMatch(d -> d.getCursoCodigo().startsWith("A"))
                && adao.obtenerPendientesDeNotificarPorDueno(profA.getId()).size() == 13;
        caso("ROL-02", "DAO: asignaciones y pendientes de envío filtrados por dueño", detB && pendA, "detB=" + detB + " pendA=" + pendA);

        for (Usuario u : new Usuario[]{profA, admin}) {
            SesionActual.iniciarSesion(u);
            FXMLLoader pr = new FXMLLoader(PruebasElvis.class.getResource("/co/edu/usc/elvis/view/Principal.fxml"));
            fx(() -> pr.load());
            boolean usuariosVis = ((Node) pr.getNamespace().get("botonUsuarios")).isVisible();
            boolean reportesVis = ((Node) pr.getNamespace().get("botonReportes")).isVisible();
            if (u == profA) caso("ROL-03", "Menú del profesor: sin Usuarios ni Reportes", !usuariosVis && !reportesVis, "usuarios=" + usuariosVis + " reportes=" + reportesVis);
            else caso("ROL-04", "Menú del administrador: con Usuarios y Reportes", usuariosVis && reportesVis, "usuarios=" + usuariosVis + " reportes=" + reportesVis);
        }

        SesionActual.iniciarSesion(profA);
        FXMLLoader ac = new FXMLLoader(PruebasElvis.class.getResource("/co/edu/usc/elvis/view/AdministrarCuentas.fxml"));
        fx(() -> ac.load());
        Object acA = ac.getController();
        boolean ocultos = !((Node) campo(acA, "panelCargarLicencias")).isVisible() && !((Node) campo(acA, "panelRegistrarCurso")).isVisible();
        @SuppressWarnings("unchecked") ComboBox<Curso> comboA = (ComboBox<Curso>) campo(acA, "comboCursoAsignar");
        List<String> enCombo = comboA.getItems().stream().map(Curso::getCodigoCurso).sorted().collect(Collectors.toList());
        @SuppressWarnings("unchecked") List<AsignacionDetalle> vistaA = (List<AsignacionDetalle>) campo(acA, "asignacionesVista");
        boolean vistaSoloA = !vistaA.isEmpty() && vistaA.stream().allMatch(d -> d.getCursoCodigo().startsWith("A"));
        caso("ROL-05", "Administrar Cuentas (profesor): oculta licencias y cursos", ocultos, null);
        caso("ROL-06", "Administrar Cuentas (profesor): solo ve sus cursos y asignaciones", enCombo.equals(List.of("A1", "A2")) && vistaSoloA, enCombo + " vistaSoloA=" + vistaSoloA);

        int antes = adao.obtenerPorCurso(b3.getId()).size();
        fx(() -> { comboA.setValue(b3); invocar(acA, "asignarLicenciasCurso"); return null; });
        String msg = ((Label) campo(acA, "etiquetaResultadoAsignacion")).getText();
        caso("ROL-07", "El profesor A no puede asignar licencias en un curso del profesor B", adao.obtenerPorCurso(b3.getId()).size() == antes && msg.contains("pertenece a otro profesor"), msg);

        SesionActual.iniciarSesion(profB);
        FXMLLoader acb = new FXMLLoader(PruebasElvis.class.getResource("/co/edu/usc/elvis/view/AdministrarCuentas.fxml"));
        fx(() -> acb.load());
        Object acB = acb.getController();
        @SuppressWarnings("unchecked") ComboBox<Curso> comboB = (ComboBox<Curso>) campo(acB, "comboCursoAsignar");
        fx(() -> { comboB.setValue(b3); invocar(acB, "asignarLicenciasCurso"); return null; });
        caso("ROL-08", "El profesor B sí puede asignar en su propio curso", adao.obtenerPorCurso(b3.getId()).size() == 1, ((Label) campo(acB, "etiquetaResultadoAsignacion")).getText());

        SesionActual.iniciarSesion(profA);
        FXMLLoader us = new FXMLLoader(PruebasElvis.class.getResource("/co/edu/usc/elvis/view/Usuarios.fxml"));
        fx(() -> us.load());
        Object usCtrl = us.getController();
        fx(() -> {
            ((javafx.scene.control.TextField) campo(usCtrl, "campoNombreCompleto")).setText("Intruso");
            ((javafx.scene.control.TextField) campo(usCtrl, "campoNombreUsuario")).setText("intruso");
            ((javafx.scene.control.TextField) campo(usCtrl, "campoContrasena")).setText("clave-intrusa");
            invocar(usCtrl, "crearUsuario");
            return null;
        });
        caso("ROL-09", "Un profesor no puede crear usuarios aunque llegue a la acción", usuario("intruso") == null, null);

        if (args.length > 1 && args[1].equals("seguridad")) {
            PruebasSeguridad.ejecutar(salida);
        }

        System.out.println("\nTOTAL: " + (ok + falla) + " casos, " + ok + " OK, " + falla + " con falla");
        Files.write(new File(salida, "resultado.txt").toPath(), filas);
        Platform.exit();
        System.exit(falla == 0 ? 0 : 1);
    }
}
