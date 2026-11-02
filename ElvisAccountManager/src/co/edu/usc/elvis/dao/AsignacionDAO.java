package co.edu.usc.elvis.dao;

import co.edu.usc.elvis.model.Asignacion;
import co.edu.usc.elvis.model.AsignacionDetalle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AsignacionDAO {

    /** Todas las asignaciones de todos los dias en UNA sola consulta (la usa el calendario). */
    public List<Asignacion> obtenerTodas() {
        List<Asignacion> lista = new ArrayList<>();
        String sql = "SELECT id, licencia_id, curso_id, estudiante_id, dia_franja, hora_inicio, hora_fin, notificado "
                + "FROM asignacion";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar asignaciones: " + e.getMessage());
        }
        return lista;
    }

    public List<Asignacion> obtenerAsignacionesPorDia(String dia) {
        List<Asignacion> lista = new ArrayList<>();
        String sql = "SELECT id, licencia_id, curso_id, estudiante_id, dia_franja, hora_inicio, hora_fin, notificado "
                + "FROM asignacion WHERE dia_franja = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dia);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar asignaciones del día: " + e.getMessage());
        }
        return lista;
    }

    public List<Asignacion> obtenerPorCurso(int cursoId) {
        List<Asignacion> lista = new ArrayList<>();
        String sql = "SELECT id, licencia_id, curso_id, estudiante_id, dia_franja, hora_inicio, hora_fin, notificado "
                + "FROM asignacion WHERE curso_id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar asignaciones del curso: " + e.getMessage());
        }
        return lista;
    }

    private static final String SQL_DETALLE_BASE =
            "SELECT a.id, a.licencia_id, a.curso_id, a.estudiante_id, a.dia_franja, a.hora_inicio, a.hora_fin, a.notificado, "
            + "datetime(a.fecha_asignacion, 'localtime') AS fecha_asignacion, "
            + "l.usuario AS licencia_usuario, l.contrasena AS licencia_contrasena, l.ip_asignada AS licencia_ip, "
            + "c.nombre_materia AS curso_nombre, c.codigo_curso AS curso_codigo, "
            + "e.nombre_completo AS estudiante_nombre, e.correo_institucional AS estudiante_correo "
            + "FROM asignacion a "
            + "JOIN licencia l ON l.id = a.licencia_id "
            + "JOIN curso c ON c.id = a.curso_id "
            + "LEFT JOIN estudiante e ON e.id = a.estudiante_id ";

    /** Todas las asignaciones (lo que ve el administrador). */
    public List<AsignacionDetalle> obtenerTodasConDetalle() {
        return consultarDetalle("ORDER BY a.dia_franja, a.hora_inicio", null);
    }

    /** Solo las asignaciones de los cursos que administra un usuario (lo que ve un profesor). */
    public List<AsignacionDetalle> obtenerConDetallePorDueno(int usuarioId) {
        return consultarDetalle("WHERE c.usuario_id = ? ORDER BY a.dia_franja, a.hora_inicio", usuarioId);
    }

    public List<AsignacionDetalle> obtenerPendientesDeNotificar() {
        return consultarDetalle("WHERE a.notificado = 0 AND a.estudiante_id IS NOT NULL "
                + "ORDER BY a.dia_franja, a.hora_inicio", null);
    }

    public List<AsignacionDetalle> obtenerPendientesDeNotificarPorDueno(int usuarioId) {
        return consultarDetalle("WHERE a.notificado = 0 AND a.estudiante_id IS NOT NULL AND c.usuario_id = ? "
                + "ORDER BY a.dia_franja, a.hora_inicio", usuarioId);
    }

    private List<AsignacionDetalle> consultarDetalle(String sufijoSql, Integer parametro) {
        List<AsignacionDetalle> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_DETALLE_BASE + sufijoSql)) {
            if (parametro != null) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearDetalle(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar asignaciones con detalle: " + e.getMessage());
        }
        return lista;
    }

    public int contarAsignacionesPorLicencia(int licenciaId) {
        String sql = "SELECT COUNT(*) AS total FROM asignacion WHERE licencia_id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, licenciaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al contar asignaciones de la licencia: " + e.getMessage());
        }
        return 0;
    }

    public void crearAsignacion(int licenciaId, int cursoId, Integer estudianteId, String dia,
                                 String horaInicio, String horaFin) throws SQLException {
        String sql = "INSERT INTO asignacion (licencia_id, curso_id, estudiante_id, dia_franja, hora_inicio, hora_fin) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, licenciaId);
            ps.setInt(2, cursoId);
            if (estudianteId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, estudianteId);
            }
            ps.setString(4, dia);
            ps.setString(5, horaInicio);
            ps.setString(6, horaFin);
            ps.executeUpdate();
        }
    }

    public void marcarNotificado(int id) throws SQLException {
        String sql = "UPDATE asignacion SET notificado = 1 WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void eliminarAsignacion(int id) throws SQLException {
        String sql = "DELETE FROM asignacion WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Asignacion mapear(ResultSet rs) throws SQLException {
        Asignacion a = new Asignacion();
        a.setId(rs.getInt("id"));
        a.setLicenciaId(rs.getInt("licencia_id"));
        a.setCursoId(rs.getInt("curso_id"));
        int estId = rs.getInt("estudiante_id");
        a.setEstudianteId(rs.wasNull() ? null : estId);
        a.setDiaFranja(rs.getString("dia_franja"));
        a.setHoraInicio(rs.getString("hora_inicio"));
        a.setHoraFin(rs.getString("hora_fin"));
        a.setNotificado(rs.getInt("notificado") == 1);
        return a;
    }

    private AsignacionDetalle mapearDetalle(ResultSet rs) throws SQLException {
        AsignacionDetalle d = new AsignacionDetalle();
        d.setId(rs.getInt("id"));
        d.setLicenciaId(rs.getInt("licencia_id"));
        d.setCursoId(rs.getInt("curso_id"));
        int estId = rs.getInt("estudiante_id");
        d.setEstudianteId(rs.wasNull() ? null : estId);
        d.setDiaFranja(rs.getString("dia_franja"));
        d.setHoraInicio(rs.getString("hora_inicio"));
        d.setHoraFin(rs.getString("hora_fin"));
        d.setNotificado(rs.getInt("notificado") == 1);
        d.setLicenciaUsuario(rs.getString("licencia_usuario"));
        d.setLicenciaContrasena(rs.getString("licencia_contrasena"));
        d.setLicenciaIp(rs.getString("licencia_ip"));
        d.setCursoNombre(rs.getString("curso_nombre"));
        d.setCursoCodigo(rs.getString("curso_codigo"));
        d.setEstudianteNombre(rs.getString("estudiante_nombre"));
        d.setEstudianteCorreo(rs.getString("estudiante_correo"));
        d.setFechaAsignacion(rs.getString("fecha_asignacion"));
        return d;
    }
}