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

    public boolean esAdministrador() {
        return ROL_ADMINISTRADOR.equals(rol);
    }
}