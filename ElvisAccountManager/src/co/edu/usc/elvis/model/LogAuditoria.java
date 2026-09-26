package co.edu.usc.elvis.model;

public class LogAuditoria {

    private int id;
    private String accion;
    private String detalle;
    private String usuario;
    private String fechaHora;

    public LogAuditoria() {
    }

    public LogAuditoria(String accion, String detalle, String usuario) {
        this.accion = accion;
        this.detalle = detalle;
        this.usuario = usuario;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getFechaHora() { return fechaHora; }
    public void setFechaHora(String fechaHora) { this.fechaHora = fechaHora; }
}