package co.edu.usc.elvis.util;

import co.edu.usc.elvis.model.Usuario;

public class SesionActual {

    private static Usuario usuarioActual;

    private SesionActual() {
    }

    public static void iniciarSesion(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static Usuario obtenerUsuario() {
        return usuarioActual;
    }

    public static boolean esAdministrador() {
        return usuarioActual != null && usuarioActual.esAdministrador();
    }

    public static void cerrarSesion() {
        usuarioActual = null;
    }
}