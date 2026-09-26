package co.edu.usc.elvis.model;

public class Asignacion {

    private int id;
    private int licenciaId;
    private int cursoId;
    private Integer estudianteId;
    private String diaFranja;
    private String horaInicio;
    private String horaFin;
    private boolean notificado;

    public Asignacion() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getLicenciaId() { return licenciaId; }
    public void setLicenciaId(int licenciaId) { this.licenciaId = licenciaId; }
    public int getCursoId() { return cursoId; }
    public void setCursoId(int cursoId) { this.cursoId = cursoId; }
    public Integer getEstudianteId() { return estudianteId; }
    public void setEstudianteId(Integer estudianteId) { this.estudianteId = estudianteId; }
    public String getDiaFranja() { return diaFranja; }
    public void setDiaFranja(String diaFranja) { this.diaFranja = diaFranja; }
    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }
    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }
    public boolean isNotificado() { return notificado; }
    public void setNotificado(boolean notificado) { this.notificado = notificado; }
}