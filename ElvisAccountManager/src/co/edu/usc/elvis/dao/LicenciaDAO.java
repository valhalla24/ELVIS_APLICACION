package co.edu.usc.elvis.dao;

import co.edu.usc.elvis.model.Licencia;
import co.edu.usc.elvis.util.CifradoUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas relacionadas con el estado de las licencias,
 * usadas principalmente para las estadisticas del panel de Inicio.
 * La contraseña de cada licencia se guarda cifrada (CifradoUtil) y se
 * entrega descifrada a quien la lee.
 */
public class LicenciaDAO {

    public int contarLicenciasTotales() {
        return ejecutarConteo("SELECT COUNT(*) AS total FROM licencia");
    }

    public int contarLicenciasAsignadas() {
        return ejecutarConteo(
                "SELECT COUNT(*) AS total FROM licencia WHERE estado IN ('Asignada', 'Compartida')");
    }

    public int contarLicenciasDisponibles() {
        return ejecutarConteo(
                "SELECT COUNT(*) AS total FROM licencia WHERE estado = 'Disponible'");
    }

    public boolean existeUsuario(String usuario) {
        String sql = "SELECT COUNT(*) AS total FROM licencia WHERE usuario = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar usuario de licencia: " + e.getMessage());
        }
        return false;
    }

    public void insertarLicencia(String usuario, String contrasena, String ip) throws SQLException {
        String sql = "INSERT INTO licencia (usuario, contrasena, ip_asignada, estado) "
                + "VALUES (?, ?, ?, 'Disponible')";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, CifradoUtil.cifrar(contrasena));
            ps.setString(3, ip);
            ps.executeUpdate();
        }
    }

    public void actualizarCredenciales(String usuario, String contrasena, String ip) throws SQLException {
        String sql = "UPDATE licencia SET contrasena = ?, ip_asignada = ? WHERE usuario = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CifradoUtil.cifrar(contrasena));
            ps.setString(2, ip);
            ps.setString(3, usuario);
            ps.executeUpdate();
        }
    }

    public List<Licencia> obtenerTodas() {
        List<Licencia> licencias = new ArrayList<>();
        String sql = "SELECT id, usuario, contrasena, ip_asignada, estado FROM licencia ORDER BY usuario";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Licencia l = new Licencia();
                l.setId(rs.getInt("id"));
                l.setUsuario(rs.getString("usuario"));
                l.setContrasena(CifradoUtil.descifrar(rs.getString("contrasena")));
                l.setIpAsignada(rs.getString("ip_asignada"));
                l.setEstado(rs.getString("estado"));
                licencias.add(l);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar licencias: " + e.getMessage());
        }
        return licencias;
    }

    public void actualizarLicenciaPorId(int id, String usuario, String contrasena, String ip) throws SQLException {
        String sql = "UPDATE licencia SET usuario = ?, contrasena = ?, ip_asignada = ? WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, CifradoUtil.cifrar(contrasena));
            ps.setString(3, ip);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    public void eliminarLicencia(int id) throws SQLException {
        String sql = "DELETE FROM licencia WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
        /**
     * Ajusta el estado de una licencia segun cuantas asignaciones activas tiene:
     * 0 = Disponible, 1 = Asignada, 2 o mas = Compartida (varios cursos en horarios distintos).
     */
    public void recalcularEstado(int licenciaId, int cantidadAsignaciones) throws SQLException {
        String nuevoEstado = cantidadAsignaciones == 0 ? "Disponible"
                : (cantidadAsignaciones == 1 ? "Asignada" : "Compartida");
        String sql = "UPDATE licencia SET estado = ? WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, licenciaId);
            ps.executeUpdate();
        }
    }

    private int ejecutarConteo(String sql) {
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar licencias: " + e.getMessage());
        }
        return 0;
    }
}