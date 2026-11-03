package co.edu.usc.elvis.dao;

import co.edu.usc.elvis.model.Usuario;
import co.edu.usc.elvis.util.SeguridadUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private static final String COLUMNAS =
            "SELECT id, nombre_completo, nombre_usuario, contrasena_hash, sal, rol, debe_cambiar_clave FROM usuario_sistema ";

    public Usuario buscarPorNombreUsuario(String nombreUsuario) {
        String sql = COLUMNAS + "WHERE nombre_usuario = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar usuario: " + e.getMessage());
        }
        return null;
    }

    public boolean existeNombreUsuario(String nombreUsuario) {
        return buscarPorNombreUsuario(nombreUsuario) != null;
    }

    public List<Usuario> obtenerTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = COLUMNAS + "ORDER BY nombre_completo";
        try (Connection conn = ConexionBD.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar usuarios: " + e.getMessage());
        }
        return lista;
    }

    /** La contraseña la elige el administrador, asi que el usuario debe cambiarla al entrar por primera vez. */
    public void crearUsuario(String nombreCompleto, String nombreUsuario, String contrasenaPlano, String rol)
            throws SQLException {
        String sal = SeguridadUtil.generarSal();
        String hash = SeguridadUtil.hashContrasena(contrasenaPlano, sal);
        String sql = "INSERT INTO usuario_sistema (nombre_completo, nombre_usuario, contrasena_hash, sal, rol, debe_cambiar_clave) "
                + "VALUES (?, ?, ?, ?, ?, 1)";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombreCompleto);
            ps.setString(2, nombreUsuario);
            ps.setString(3, hash);
            ps.setString(4, sal);
            ps.setString(5, rol);
            ps.executeUpdate();
        }
    }

    public void cambiarRol(int id, String nuevoRol) throws SQLException {
        String sql = "UPDATE usuario_sistema SET rol = ? WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoRol);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** El administrador pone una clave temporal: el usuario debe cambiarla al entrar. */
    public void restablecerContrasena(int id, String nuevaContrasenaPlano) throws SQLException {
        guardarContrasena(id, nuevaContrasenaPlano, true);
    }

    /** El propio usuario elige su contraseña (o se re-guarda la misma con el algoritmo actual). */
    public void cambiarContrasenaPropia(int id, String nuevaContrasenaPlano) throws SQLException {
        guardarContrasena(id, nuevaContrasenaPlano, false);
    }

    /** Re-guarda la misma contraseña con el algoritmo actual sin tocar la marca de cambio obligatorio. */
    public void actualizarHash(int id, String contrasenaPlano) throws SQLException {
        guardarContrasena(id, contrasenaPlano, null);
    }

    private void guardarContrasena(int id, String contrasenaPlano, Boolean debeCambiar) throws SQLException {
        String sal = SeguridadUtil.generarSal();
        String hash = SeguridadUtil.hashContrasena(contrasenaPlano, sal);
        String sql = "UPDATE usuario_sistema SET contrasena_hash = ?, sal = ?"
                + (debeCambiar == null ? "" : ", debe_cambiar_clave = " + (debeCambiar ? 1 : 0))
                + " WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, sal);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina la cuenta. Los cursos que administraba NO se borran: quedan
     * "sin dueño" hasta que el administrador los reasigne a otro usuario.
     */
    public void eliminarUsuario(int id) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM usuario_sistema WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE curso SET usuario_id = NULL WHERE usuario_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setNombreUsuario(rs.getString("nombre_usuario"));
        u.setContrasenaHash(rs.getString("contrasena_hash"));
        u.setSal(rs.getString("sal"));
        u.setRol(rs.getString("rol"));
        u.setDebeCambiarClave(rs.getInt("debe_cambiar_clave") == 1);
        return u;
    }
}