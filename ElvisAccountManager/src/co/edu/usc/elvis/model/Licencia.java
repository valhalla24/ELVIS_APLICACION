package co.edu.usc.elvis.model;

public class Licencia {

    private int id;
    private String usuario;
    private String contrasena;
    private String ipAsignada;
    private String estado;
    private String fechaAsignacion;
    private String vigenciaHasta;

    public Licencia() {
    }

    public Licencia(String usuario, String contrasena, String ipAsignada, String estado) {
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.ipAsignada = ipAsignada;
        this.estado = estado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public String getIpAsignada() { return ipAsignada; }
    public void setIpAsignada(String ipAsignada) { this.ipAsignada = ipAsignada; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(String fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }
    public String getVigenciaHasta() { return vigenciaHasta; }
    public void setVigenciaHasta(String vigenciaHasta) { this.vigenciaHasta = vigenciaHasta; }
}