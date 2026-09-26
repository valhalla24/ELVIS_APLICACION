package co.edu.usc.elvis.model;

public class AsignacionDetalle {

    private int id;
    private int licenciaId;
    private int cursoId;
    private Integer estudianteId;
    private String cursoNombre;
    private String cursoCodigo;
    private String licenciaUsuario;
    private String licenciaContrasena;
    private String licenciaIp;
    private String estudianteNombre;
    private String estudianteCorreo;
    private String diaFranja;
    private String horaInicio;
    private String horaFin;
    private boolean notificado;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getLicenciaId() { return licenciaId; }
    public void setLicenciaId(int licenciaId) { this.licenciaId = licenciaId; }
    public int getCursoId() { return cursoId; }
    public void setCursoId(int cursoId) { this.cursoId = cursoId; }
    public Integer getEstudianteId() { return estudianteId; }
    public void setEstudianteId(Integer estudianteId) { this.estudianteId = estudianteId; }
    public String getCursoNombre() { return cursoNombre; }
    public void setCursoNombre(String cursoNombre) { this.cursoNombre = cursoNombre; }
    public String getCursoCodigo() { return cursoCodigo; }
    public void setCursoCodigo(String cursoCodigo) { this.cursoCodigo = cursoCodigo; }
    public String getLicenciaUsuario() { return licenciaUsuario; }
    public void setLicenciaUsuario(String licenciaUsuario) { this.licenciaUsuario = licenciaUsuario; }
    public String getLicenciaContrasena() { return licenciaContrasena; }
    public void setLicenciaContrasena(String licenciaContrasena) { this.licenciaContrasena = licenciaContrasena; }
    public String getLicenciaIp() { return licenciaIp; }
    public void setLicenciaIp(String licenciaIp) { this.licenciaIp = licenciaIp; }
    public String getEstudianteNombre() { return estudianteNombre; }
    public void setEstudianteNombre(String estudianteNombre) { this.estudianteNombre = estudianteNombre; }
    public String getEstudianteCorreo() { return estudianteCorreo; }
    public void setEstudianteCorreo(String estudianteCorreo) { this.estudianteCorreo = estudianteCorreo; }
    public String getDiaFranja() { return diaFranja; }
    public void setDiaFranja(String diaFranja) { this.diaFranja = diaFranja; }
    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }
    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }
    public boolean isNotificado() { return notificado; }
    public void setNotificado(boolean notificado) { this.notificado = notificado; }

    public String getEstadoEnvio() {
        return notificado ? "Enviado" : "Pendiente";
    }
}