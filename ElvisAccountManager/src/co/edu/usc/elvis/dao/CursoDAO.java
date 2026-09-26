package co.edu.usc.elvis.dao;

import co.edu.usc.elvis.model.Curso;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CursoDAO {

    public boolean registrarCurso(Curso curso) throws SQLException {
        String sql = "INSERT INTO curso (nombre_materia, codigo_curso, semestre_lectivo, "
                + "docente_responsable, dia_clase, hora_inicio, hora_fin, licencias_requeridas) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, curso.getNombreMateria());
            ps.setString(2, curso.getCodigoCurso());
            ps.setString(3, curso.getSemestreLectivo());
            ps.setString(4, curso.getDocenteResponsable());
            ps.setString(5, curso.getDiaClase());
            ps.setString(6, curso.getHoraInicio());
            ps.setString(7, curso.getHoraFin());
            ps.setInt(8, curso.getLicenciasRequeridas());
            ps.executeUpdate();

            try (Statement stmtId = conn.createStatement();
                 ResultSet rs = stmtId.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    curso.setId(rs.getInt(1));
                }
            }
            return true;
        }
    }

    public boolean existeCodigoCurso(String codigoCurso) {
        String sql = "SELECT COUNT(*) AS total FROM curso WHERE codigo_curso = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigoCurso);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar código de curso: " + e.getMessage());
        }
        return false;
    }

    public List<Curso> obtenerTodos() {
        List<Curso> cursos = new ArrayList<>();
        String sql = "SELECT id, nombre_materia, codigo_curso, semestre_lectivo, docente_responsable, "
                + "dia_clase, hora_inicio, hora_fin, licencias_requeridas FROM curso ORDER BY nombre_materia";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Curso curso = new Curso();
                curso.setId(rs.getInt("id"));
                curso.setNombreMateria(rs.getString("nombre_materia"));
                curso.setCodigoCurso(rs.getString("codigo_curso"));
                curso.setSemestreLectivo(rs.getString("semestre_lectivo"));
                curso.setDocenteResponsable(rs.getString("docente_responsable"));
                curso.setDiaClase(rs.getString("dia_clase"));
                curso.setHoraInicio(rs.getString("hora_inicio"));
                curso.setHoraFin(rs.getString("hora_fin"));
                curso.setLicenciasRequeridas(rs.getInt("licencias_requeridas"));
                cursos.add(curso);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar cursos: " + e.getMessage());
        }
        return cursos;
    }

    public boolean existeCodigoCursoExcluyendo(String codigoCurso, int idExcluir) {
        String sql = "SELECT COUNT(*) AS total FROM curso WHERE codigo_curso = ? AND id != ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigoCurso);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar código de curso: " + e.getMessage());
        }
        return false;
    }

    public void actualizarCurso(Curso curso) throws SQLException {
        String sql = "UPDATE curso SET nombre_materia = ?, codigo_curso = ?, semestre_lectivo = ?, "
                + "docente_responsable = ?, dia_clase = ?, hora_inicio = ?, hora_fin = ?, licencias_requeridas = ? "
                + "WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, curso.getNombreMateria());
            ps.setString(2, curso.getCodigoCurso());
            ps.setString(3, curso.getSemestreLectivo());
            ps.setString(4, curso.getDocenteResponsable());
            ps.setString(5, curso.getDiaClase());
            ps.setString(6, curso.getHoraInicio());
            ps.setString(7, curso.getHoraFin());
            ps.setInt(8, curso.getLicenciasRequeridas());
            ps.setInt(9, curso.getId());
            ps.executeUpdate();
        }
    }

    public void eliminarCurso(int id) throws SQLException {
        String sql = "DELETE FROM curso WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}