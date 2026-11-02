package co.edu.usc.elvis.dao;

import co.edu.usc.elvis.model.Estudiante;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EstudianteDAO {

    public int contarTodos() {
        String sql = "SELECT COUNT(*) AS total FROM estudiante";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("Error al contar estudiantes: " + e.getMessage());
        }
        return 0;
    }

    /** Estudiantes que hay en los cursos de un usuario (una sola consulta). */
    public int contarPorDueno(int usuarioId) {
        String sql = "SELECT COUNT(*) AS total FROM estudiante e "
                + "JOIN curso c ON c.id = e.curso_id WHERE c.usuario_id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al contar estudiantes del usuario: " + e.getMessage());
        }
        return 0;
    }

    public boolean existeCodigoEstudiantil(String codigo) {
        String sql = "SELECT COUNT(*) AS total FROM estudiante WHERE codigo_estudiantil = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar código de estudiante: " + e.getMessage());
        }
        return false;
    }

    /** Curso en el que esta registrado hoy un estudiante (null si no existe o no tiene curso). */
    public Integer obtenerCursoIdPorCodigo(String codigo) {
        String sql = "SELECT curso_id FROM estudiante WHERE codigo_estudiantil = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int cursoId = rs.getInt("curso_id");
                    return rs.wasNull() ? null : cursoId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar el curso del estudiante: " + e.getMessage());
        }
        return null;
    }

    public void insertarEstudiante(Estudiante estudiante) throws SQLException {
        String sql = "INSERT INTO estudiante (nombre_completo, codigo_estudiantil, correo_institucional, curso_id) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estudiante.getNombreCompleto());
            ps.setString(2, estudiante.getCodigoEstudiantil());
            ps.setString(3, estudiante.getCorreoInstitucional());
            ps.setInt(4, estudiante.getCursoId());
            ps.executeUpdate();
        }
    }

    public void actualizarEstudiante(Estudiante estudiante) throws SQLException {
        String sql = "UPDATE estudiante SET nombre_completo = ?, correo_institucional = ?, curso_id = ? "
                + "WHERE codigo_estudiantil = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estudiante.getNombreCompleto());
            ps.setString(2, estudiante.getCorreoInstitucional());
            ps.setInt(3, estudiante.getCursoId());
            ps.setString(4, estudiante.getCodigoEstudiantil());
            ps.executeUpdate();
        }
    }

    public int contarEstudiantesPorCurso(int cursoId) {
        String sql = "SELECT COUNT(*) AS total FROM estudiante WHERE curso_id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al contar estudiantes del curso: " + e.getMessage());
        }
        return 0;
    }

    public List<Estudiante> obtenerPorCurso(int cursoId) {
        List<Estudiante> estudiantes = new ArrayList<>();
        String sql = "SELECT id, nombre_completo, codigo_estudiantil, correo_institucional, curso_id "
                + "FROM estudiante WHERE curso_id = ? ORDER BY nombre_completo";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cursoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Estudiante e = new Estudiante();
                    e.setId(rs.getInt("id"));
                    e.setNombreCompleto(rs.getString("nombre_completo"));
                    e.setCodigoEstudiantil(rs.getString("codigo_estudiantil"));
                    e.setCorreoInstitucional(rs.getString("correo_institucional"));
                    e.setCursoId(rs.getInt("curso_id"));
                    estudiantes.add(e);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar estudiantes del curso: " + e.getMessage());
        }
        return estudiantes;
    }

    public void actualizarEstudiantePorId(int id, String nombreCompleto, String codigoEstudiantil,
                                           String correoInstitucional) throws SQLException {
        String sql = "UPDATE estudiante SET nombre_completo = ?, codigo_estudiantil = ?, correo_institucional = ? "
                + "WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreCompleto);
            ps.setString(2, codigoEstudiantil);
            ps.setString(3, correoInstitucional);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    public void eliminarEstudiante(int id) throws SQLException {
        String sql = "DELETE FROM estudiante WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}