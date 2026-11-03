package co.edu.usc.elvis.model;

public class Usuario {

    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    public static final String ROL_PROFESOR = "PROFESOR";

    private int id;
    private String nombreCompleto;
    private String nombreUsuario;
    private String contrasenaHash;
    private String sal;
    private String rol;
    private boolean debeCambiarClave;

    public Usuario() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }
    public String getSal() { return sal; }
    public void setSal(String sal) { this.sal = sal; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    /** True si debe elegir una contraseña nueva al iniciar sesión (cuenta nueva o clave restablecida). */
    public boolean isDebeCambiarClave() { return debeCambiarClave; }
    public void setDebeCambiarClave(boolean debeCambiarClave) { this.debeCambiarClave = debeCambiarClave; }

    public boolean esAdministrador() {
        return ROL_ADMINISTRADOR.equals(rol);
    }
}