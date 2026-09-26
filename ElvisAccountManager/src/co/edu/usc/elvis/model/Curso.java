package co.edu.usc.elvis.model;

public class Curso {

    private int id;
    private String nombreMateria;
    private String codigoCurso;
    private String semestreLectivo;
    private String docenteResponsable;
    private String diaClase;
    private String horaInicio;
    private String horaFin;
    private int licenciasRequeridas;

    public Curso() {
    }

    public Curso(String nombreMateria, String codigoCurso, String semestreLectivo,
                 String docenteResponsable, String diaClase, String horaInicio,
                 String horaFin, int licenciasRequeridas) {
        this.nombreMateria = nombreMateria;
        this.codigoCurso = codigoCurso;
        this.semestreLectivo = semestreLectivo;
        this.docenteResponsable = docenteResponsable;
        this.diaClase = diaClase;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.licenciasRequeridas = licenciasRequeridas;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombreMateria() { return nombreMateria; }
    public void setNombreMateria(String nombreMateria) { this.nombreMateria = nombreMateria; }
    public String getCodigoCurso() { return codigoCurso; }
    public void setCodigoCurso(String codigoCurso) { this.codigoCurso = codigoCurso; }
    public String getSemestreLectivo() { return semestreLectivo; }
    public void setSemestreLectivo(String semestreLectivo) { this.semestreLectivo = semestreLectivo; }
    public String getDocenteResponsable() { return docenteResponsable; }
    public void setDocenteResponsable(String docenteResponsable) { this.docenteResponsable = docenteResponsable; }
    public String getDiaClase() { return diaClase; }
    public void setDiaClase(String diaClase) { this.diaClase = diaClase; }
    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }
    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }
    public int getLicenciasRequeridas() { return licenciasRequeridas; }
    public void setLicenciasRequeridas(int licenciasRequeridas) { this.licenciasRequeridas = licenciasRequeridas; }
}