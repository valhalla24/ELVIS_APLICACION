package co.edu.usc.elvis.util;

import co.edu.usc.elvis.dao.AsignacionDAO;
import co.edu.usc.elvis.dao.CursoDAO;
import co.edu.usc.elvis.dao.EstudianteDAO;
import co.edu.usc.elvis.dao.LicenciaDAO;
import co.edu.usc.elvis.dao.LogAuditoriaDAO;
import co.edu.usc.elvis.model.Asignacion;
import co.edu.usc.elvis.model.AsignacionDetalle;
import co.edu.usc.elvis.model.Curso;
import co.edu.usc.elvis.model.ReporteDatos;
import co.edu.usc.elvis.model.ReporteDatos.TipoColumna;
import co.edu.usc.elvis.model.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Calcula el contenido de los reportes. No sabe nada de pantallas ni de
 * archivos: devuelve un ReporteDatos que luego se muestra o se exporta.
 *
 * Alcance por rol: el administrador ve todo; un profesor solo lo de sus
 * cursos. Los reportes NUNCA incluyen contraseñas de licencias.
 */
public class ReporteService {

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};
    private static final double UMBRAL_ALTA = 0.85;
    private static final double UMBRAL_MODERADA = 0.70;
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CursoDAO cursoDAO = new CursoDAO();
    private final AsignacionDAO asignacionDAO = new AsignacionDAO();
    private final EstudianteDAO estudianteDAO = new EstudianteDAO();
    private final LicenciaDAO licenciaDAO = new LicenciaDAO();
    private final LogAuditoriaDAO logAuditoriaDAO = new LogAuditoriaDAO();

    // ------------------------------------------------ 1. Ocupacion por curso (foto actual)

    public ReporteDatos ocupacionPorCurso(Usuario usuario, String dependencia) {
        boolean admin = usuario.esAdministrador();
        ReporteDatos d = new ReporteDatos("ocupacion_por_curso", "Ocupación de licencias por curso");
        d.setNombreHoja("Ocupación por curso");
        encabezadoComun(d, usuario, dependencia,
                admin ? "Alcance: todos los cursos del sistema" : "Alcance: solo los cursos de este profesor", null);

        d.agregarColumna("Código", TipoColumna.TEXTO, 1.0);
        d.agregarColumna("Materia", TipoColumna.TEXTO, 2.2);
        d.agregarColumna("Docente", TipoColumna.TEXTO, 1.8);
        if (admin) {
            d.agregarColumna("Dueño", TipoColumna.TEXTO, 1.5);
        }
        d.agregarColumna("Día", TipoColumna.TEXTO, 1.0);
        d.agregarColumna("Horario", TipoColumna.TEXTO, 1.7);
        d.agregarColumna("Estudiantes", TipoColumna.ENTERO, 1.0);
        d.agregarColumna("Requeridas", TipoColumna.ENTERO, 1.0);
        d.agregarColumna("Asignadas", TipoColumna.ENTERO, 1.0);
        d.agregarColumna("% del cupo total", TipoColumna.PORCENTAJE, 1.2);

        int totalLicencias = licenciaDAO.contarLicenciasTotales();
        List<Curso> cursos = cursosDe(usuario);
        cursos.sort(Comparator.comparing(Curso::getCodigoCurso));
        int asignacionesTotales = 0;
        int conLicencias = 0;

        for (Curso c : cursos) {
            int asignadas = asignacionDAO.obtenerPorCurso(c.getId()).size();
            int estudiantes = estudianteDAO.contarEstudiantesPorCurso(c.getId());
            double porcentaje = totalLicencias == 0 ? 0 : asignadas * 100.0 / totalLicencias;
            asignacionesTotales += asignadas;
            if (asignadas > 0) {
                conLicencias++;
            }

            List<Object> fila = new ArrayList<>();
            fila.add(c.getCodigoCurso());
            fila.add(c.getNombreMateria());
            fila.add(c.getDocenteResponsable());
            if (admin) {
                fila.add(c.getDuenoTexto());
            }
            fila.add(c.getDiaClase());
            fila.add(c.getHoraInicio() + " - " + c.getHoraFin());
            fila.add(estudiantes);
            fila.add(c.getLicenciasRequeridas());
            fila.add(asignadas);
            fila.add(porcentaje);
            d.agregarFila(fila.toArray());
        }

        d.agregarResumen("Licencias en el sistema: " + totalLicencias);
        d.agregarResumen("Cursos incluidos: " + cursos.size() + " · con licencias asignadas: " + conLicencias
                + " · sin asignar: " + (cursos.size() - conLicencias));
        d.agregarResumen("Asignaciones de licencia (suma de todos los cursos): " + asignacionesTotales);
        return d;
    }

    // ------------------------------------------------ 2. Asignaciones por estudiante

    public ReporteDatos asignaciones(Usuario usuario, String dependencia, LocalDate desde, LocalDate hasta) {
        boolean admin = usuario.esAdministrador();
        ReporteDatos d = new ReporteDatos("asignaciones", "Asignaciones de licencias por estudiante");
        d.setNombreHoja("Asignaciones");
        encabezadoComun(d, usuario, dependencia,
                admin ? "Alcance: todos los cursos del sistema" : "Alcance: solo los cursos de este profesor",
                textoRango(desde, hasta, "fecha de asignación"));

        d.agregarColumna("Código curso", TipoColumna.TEXTO, 1.1);
        d.agregarColumna("Materia", TipoColumna.TEXTO, 2.0);
        d.agregarColumna("Estudiante", TipoColumna.TEXTO, 2.2);
        d.agregarColumna("Licencia", TipoColumna.TEXTO, 1.6);
        d.agregarColumna("Día", TipoColumna.TEXTO, 1.0);
        d.agregarColumna("Horario", TipoColumna.TEXTO, 1.7);
        d.agregarColumna("Envío", TipoColumna.TEXTO, 0.9);
        d.agregarColumna("Fecha de asignación", TipoColumna.TEXTO, 1.5);

        List<AsignacionDetalle> detalles = admin
                ? asignacionDAO.obtenerTodasConDetalle()
                : asignacionDAO.obtenerConDetallePorDueno(usuario.getId());

        List<AsignacionDetalle> filtradas = new ArrayList<>();
        for (AsignacionDetalle a : detalles) {
            if (dentroDelRango(a.getFechaAsignacion(), desde, hasta)) {
                filtradas.add(a);
            }
        }
        filtradas.sort(Comparator
                .comparing((AsignacionDetalle a) -> a.getCursoCodigo() == null ? "" : a.getCursoCodigo())
                .thenComparing(a -> a.getEstudianteNombre() == null ? "" : a.getEstudianteNombre()));

        int enviadas = 0;
        for (AsignacionDetalle a : filtradas) {
            if (a.isNotificado()) {
                enviadas++;
            }
            d.agregarFila(a.getCursoCodigo(), a.getCursoNombre(),
                    a.getEstudianteNombre() == null ? "(sin estudiante)" : a.getEstudianteNombre(),
                    a.getLicenciaUsuario(), a.getDiaFranja(),
                    a.getHoraInicio() + " - " + a.getHoraFin(),
                    a.getEstadoEnvio(),
                    a.getFechaAsignacion() == null ? "" : a.getFechaAsignacion());
        }

        d.agregarResumen("Asignaciones en el reporte: " + filtradas.size());
        d.agregarResumen("Credenciales enviadas: " + enviadas + " · pendientes de envío: " + (filtradas.size() - enviadas));
        return d;
    }

    // ------------------------------------------------ 3. Ocupacion por franja (foto actual, global)

    public ReporteDatos ocupacionPorFranja(Usuario usuario, String dependencia) {
        ReporteDatos d = new ReporteDatos("ocupacion_por_franja", "Ocupación de licencias por franja horaria");
        d.setNombreHoja("Ocupación por franja");
        encabezadoComun(d, usuario, dependencia, "Alcance: todos los cursos del sistema (igual que el calendario)", null);

        d.agregarColumna("Día", TipoColumna.TEXTO, 1.2);
        d.agregarColumna("Hora", TipoColumna.TEXTO, 1.2);
        d.agregarColumna("Licencias en uso", TipoColumna.ENTERO, 1.2);
        d.agregarColumna("Capacidad", TipoColumna.ENTERO, 1.0);
        d.agregarColumna("Ocupación", TipoColumna.PORCENTAJE, 1.1);
        d.agregarColumna("Nivel", TipoColumna.TEXTO, 1.0);
        d.agregarColumna("Cursos en la franja", TipoColumna.TEXTO, 3.2);

        int total = licenciaDAO.contarLicenciasTotales();
        List<Curso> cursos = cursoDAO.obtenerTodos();
        List<String> horas = HorarioUtil.generarFranjasHorarias();

        int franjasOcupadas = 0;
        int franjasAltas = 0;
        int picoUso = 0;
        String picoDia = "";
        String picoHora = "";

        if (total > 0) {
            for (String dia : DIAS) {
                List<Asignacion> delDia = asignacionDAO.obtenerAsignacionesPorDia(dia);
                for (String hora : horas) {
                    int uso = 0;
                    for (Asignacion a : delDia) {
                        if (HorarioUtil.incluyeHora(a.getHoraInicio(), a.getHoraFin(), hora)) {
                            uso++;
                        }
                    }
                    if (uso == 0) {
                        continue;
                    }
                    double ocupacion = (double) uso / total;
                    String nivel = ocupacion > UMBRAL_ALTA ? "Alta" : (ocupacion >= UMBRAL_MODERADA ? "Moderada" : "Normal");

                    Set<String> codigos = new HashSet<>();
                    List<String> codigosOrdenados = new ArrayList<>();
                    for (Curso c : cursos) {
                        if (c.getDiaClase().equals(dia)
                                && HorarioUtil.incluyeHora(c.getHoraInicio(), c.getHoraFin(), hora)
                                && codigos.add(c.getCodigoCurso())) {
                            codigosOrdenados.add(c.getCodigoCurso());
                        }
                    }
                    codigosOrdenados.sort(String::compareTo);

                    d.agregarFila(dia, hora, uso, total, ocupacion * 100, nivel, String.join(", ", codigosOrdenados));
                    franjasOcupadas++;
                    if ("Alta".equals(nivel)) {
                        franjasAltas++;
                    }
                    if (uso > picoUso) {
                        picoUso = uso;
                        picoDia = dia;
                        picoHora = hora;
                    }
                }
            }
        }

        d.agregarResumen("Capacidad total: " + total + " licencias");
        if (total == 0) {
            d.agregarResumen("Aún no hay licencias cargadas en el sistema.");
        } else if (franjasOcupadas == 0) {
            d.agregarResumen("Todavía no hay licencias asignadas a ningún curso.");
        } else {
            d.agregarResumen(String.format("Pico máximo de cuota: %.1f%% (%d/%d licencias) — %s %s",
                    picoUso * 100.0 / total, picoUso, total, picoDia, picoHora));
            d.agregarResumen("Franjas ocupadas: " + franjasOcupadas + " · en nivel Alta (más de 85%): " + franjasAltas);
        }
        return d;
    }

    // ------------------------------------------------ 4. Bitacora de auditoria (solo administrador)

    public ReporteDatos bitacora(Usuario usuario, String dependencia, LocalDate desde, LocalDate hasta) {
        if (usuario == null || !usuario.esAdministrador()) {
            throw new IllegalArgumentException("La bitácora de auditoría es solo para el administrador.");
        }
        ReporteDatos d = new ReporteDatos("bitacora_auditoria", "Bitácora de auditoría");
        d.setNombreHoja("Bitácora");
        encabezadoComun(d, usuario, dependencia, "Alcance: todos los eventos del sistema",
                textoRango(desde, hasta, "fecha del evento"));

        d.agregarColumna("Fecha y hora", TipoColumna.TEXTO, 1.5);
        d.agregarColumna("Acción", TipoColumna.TEXTO, 1.7);
        d.agregarColumna("Usuario", TipoColumna.TEXTO, 1.0);
        d.agregarColumna("Detalle", TipoColumna.TEXTO, 4.5);

        List<LogAuditoriaDAO.EntradaLog> entradas = logAuditoriaDAO.obtenerPorRango(
                desde == null ? null : desde.toString(), hasta == null ? null : hasta.toString());
        for (LogAuditoriaDAO.EntradaLog e : entradas) {
            d.agregarFila(e.fechaHora, nombreLegibleDeAccion(e.accion),
                    e.usuario == null ? "" : e.usuario, e.detalle == null ? "" : e.detalle);
        }
        d.agregarResumen("Eventos en el reporte: " + entradas.size());
        return d;
    }

    // ------------------------------------------------ utilidades

    private List<Curso> cursosDe(Usuario usuario) {
        return usuario.esAdministrador() ? cursoDAO.obtenerTodos() : cursoDAO.obtenerPorDueno(usuario.getId());
    }

    private void encabezadoComun(ReporteDatos d, Usuario usuario, String dependencia, String alcance, String rango) {
        String dep = (dependencia == null || dependencia.isBlank()) ? "Facultad de Ingeniería" : dependencia.trim();
        d.agregarLineaEncabezado("Dependencia solicitante: " + dep);
        d.agregarLineaEncabezado("Generado el " + FORMATO_FECHA_HORA.format(LocalDateTime.now()) + " por "
                + usuario.getNombreCompleto() + " (" + (usuario.esAdministrador() ? "Administrador" : "Profesor") + ")");
        if (alcance != null) {
            d.agregarLineaEncabezado(alcance);
        }
        if (rango != null) {
            d.agregarLineaEncabezado(rango);
        }
    }

    private String textoRango(LocalDate desde, LocalDate hasta, String campo) {
        if (desde == null && hasta == null) {
            return "Rango por " + campo + ": todo el historial";
        }
        return "Rango por " + campo + ": "
                + (desde == null ? "sin límite inferior" : FORMATO_FECHA.format(desde))
                + " a "
                + (hasta == null ? "sin límite superior" : FORMATO_FECHA.format(hasta));
    }

    /** fechaTexto viene como "yyyy-MM-dd HH:mm:ss" (hora local). */
    static boolean dentroDelRango(String fechaTexto, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null) {
            return true;
        }
        if (fechaTexto == null || fechaTexto.length() < 10) {
            return false;
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaTexto.substring(0, 10));
        } catch (RuntimeException e) {
            return false;
        }
        return (desde == null || !fecha.isBefore(desde)) && (hasta == null || !fecha.isAfter(hasta));
    }

    static String nombreLegibleDeAccion(String accion) {
        if (accion == null) {
            return "";
        }
        switch (accion) {
            case "carga_licencias": return "Carga de licencias";
            case "carga_estudiantes": return "Carga de lista de estudiantes";
            case "asignacion_licencias": return "Asignación de licencias";
            case "envio_credenciales": return "Envío de credenciales";
            case "reenvio_credenciales": return "Reenvío de credenciales";
            case "licencia_editada": return "Licencia editada";
            case "licencia_eliminada": return "Licencia eliminada";
            case "usuario_creado": return "Usuario creado";
            case "usuario_rol_cambiado": return "Cambio de rol";
            case "usuario_clave_restablecida": return "Contraseña restablecida";
            case "usuario_clave_cambiada": return "Contraseña cambiada por el usuario";
            case "usuario_eliminado": return "Usuario eliminado";
            case "reporte_exportado": return "Reporte exportado";
            default: return accion;
        }
    }
}