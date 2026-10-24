package co.edu.usc.elvis.dao;

import co.edu.usc.elvis.model.Curso;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CursoDAO {

    private static final String SQL_SELECT_CURSO =
            "SELECT c.id, c.nombre_materia, c.codigo_curso, c.semestre_lectivo, c.docente_responsable, "
            + "c.dia_clase, c.hora_inicio, c.hora_fin, c.licencias_requeridas, c.usuario_id, "
            + "u.nombre_completo AS dueno_nombre "
            + "FROM curso c LEFT JOIN usuario_sistema u ON u.id = c.usuario_id ";

    public boolean registrarCurso(Curso curso) throws SQLException {
        String sql = "INSERT INTO curso (nombre_materia, codigo_curso, semestre_lectivo, "
                + "docente_responsable, dia_clase, hora_inicio, hora_fin, licencias_requeridas, usuario_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
            asignarDueno(ps, 9, curso.getUsuarioId());
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

    /** Todos los cursos (para el administrador y para el calendario). */
    public List<Curso> obtenerTodos() {
        return listar(SQL_SELECT_CURSO + "ORDER BY c.nombre_materia", null);
    }

    /** Solo los cursos que administra un usuario (lo que ve un profesor). */
    public List<Curso> obtenerPorDueno(int usuarioId) {
        return listar(SQL_SELECT_CURSO + "WHERE c.usuario_id = ? ORDER BY c.nombre_materia", usuarioId);
    }

    public Curso obtenerPorId(int id) {
        List<Curso> resultado = listar(SQL_SELECT_CURSO + "WHERE c.id = ?", id);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public int contarPorDueno(int usuarioId) {
        String sql = "SELECT COUNT(*) AS total FROM curso WHERE usuario_id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al contar cursos del usuario: " + e.getMessage());
        }
        return 0;
    }

    public void actualizarCurso(Curso curso) throws SQLException {
        String sql = "UPDATE curso SET nombre_materia = ?, codigo_curso = ?, semestre_lectivo = ?, "
                + "docente_responsable = ?, dia_clase = ?, hora_inicio = ?, hora_fin = ?, "
                + "licencias_requeridas = ?, usuario_id = ? WHERE id = ?";
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
            asignarDueno(ps, 9, curso.getUsuarioId());
            ps.setInt(10, curso.getId());
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

    private void asignarDueno(PreparedStatement ps, int indice, Integer usuarioId) throws SQLException {
        if (usuarioId == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, usuarioId);
        }
    }

    private List<Curso> listar(String sql, Integer parametro) {
        List<Curso> cursos = new ArrayList<>();
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cursos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar cursos: " + e.getMessage());
        }
        return cursos;
    }

    private Curso mapear(ResultSet rs) throws SQLException {
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
        int duenoId = rs.getInt("usuario_id");
        curso.setUsuarioId(rs.wasNull() ? null : duenoId);
        curso.setDuenoNombre(rs.getString("dueno_nombre"));
        return curso;
    }
}