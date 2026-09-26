package co.edu.usc.elvis.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LogAuditoriaDAO {

    public static class EventoReciente {
        public String detalle;
        public String fechaHora;
    }

    public void registrarEvento(String accion, String detalle, String usuario) {
        String sql = "INSERT INTO log_auditoria (accion, detalle, usuario) VALUES (?, ?, ?)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accion);
            ps.setString(2, detalle);
            ps.setString(3, usuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al registrar evento de auditoría: " + e.getMessage());
        }
    }

    public EventoReciente obtenerUltimoEvento(String accion) {
        String sql = "SELECT detalle, fecha_hora FROM log_auditoria WHERE accion = ? "
                + "ORDER BY fecha_hora DESC, id DESC LIMIT 1";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EventoReciente evento = new EventoReciente();
                    evento.detalle = rs.getString("detalle");
                    evento.fechaHora = rs.getString("fecha_hora");
                    return evento;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar último evento: " + e.getMessage());
        }
        return null;
    }
}