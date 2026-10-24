package co.edu.usc.elvis.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LogAuditoriaDAO {

    public static class EventoReciente {
        public String detalle;
        public String fechaHora;
    }

    public static class EntradaLog {
        public String fechaHora;
        public String accion;
        public String detalle;
        public String usuario;
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

    /** Ultimo evento de un tipo; la fecha se devuelve en la hora local del equipo. */
    public EventoReciente obtenerUltimoEvento(String accion) {
        String sql = "SELECT detalle, datetime(fecha_hora, 'localtime') AS fecha_local FROM log_auditoria "
                + "WHERE accion = ? ORDER BY fecha_hora DESC, id DESC LIMIT 1";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    EventoReciente evento = new EventoReciente();
                    evento.detalle = rs.getString("detalle");
                    evento.fechaHora = rs.getString("fecha_local");
                    return evento;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar último evento: " + e.getMessage());
        }
        return null;
    }

    /**
     * Bitacora completa, del mas reciente al mas antiguo. Las fechas desde/hasta
     * (formato yyyy-MM-dd, ambas opcionales e inclusivas) se interpretan en hora local.
     */
    public List<EntradaLog> obtenerPorRango(String desde, String hasta) {
        List<EntradaLog> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT datetime(fecha_hora, 'localtime') AS fecha_local, accion, detalle, usuario "
                + "FROM log_auditoria WHERE 1 = 1 ");
        if (desde != null) {
            sql.append("AND date(fecha_hora, 'localtime') >= ? ");
        }
        if (hasta != null) {
            sql.append("AND date(fecha_hora, 'localtime') <= ? ");
        }
        sql.append("ORDER BY fecha_hora DESC, id DESC");

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int indice = 1;
            if (desde != null) {
                ps.setString(indice++, desde);
            }
            if (hasta != null) {
                ps.setString(indice, hasta);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EntradaLog e = new EntradaLog();
                    e.fechaHora = rs.getString("fecha_local");
                    e.accion = rs.getString("accion");
                    e.detalle = rs.getString("detalle");
                    e.usuario = rs.getString("usuario");
                    lista.add(e);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar la bitácora: " + e.getMessage());
        }
        return lista;
    }
}