package co.edu.usc.elvis.model;

public class Estudiante {

    private int id;
    private String nombreCompleto;
    private String codigoEstudiantil;
    private String correoInstitucional;
    private int cursoId;

    public Estudiante() {
    }

    public Estudiante(String nombreCompleto, String codigoEstudiantil,
                       String correoInstitucional, int cursoId) {
        this.nombreCompleto = nombreCompleto;
        this.codigoEstudiantil = codigoEstudiantil;
        this.correoInstitucional = correoInstitucional;
        this.cursoId = cursoId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCodigoEstudiantil() { return codigoEstudiantil; }
    public void setCodigoEstudiantil(String codigoEstudiantil) { this.codigoEstudiantil = codigoEstudiantil; }
    public String getCorreoInstitucional() { return correoInstitucional; }
    public void setCorreoInstitucional(String correoInstitucional) { this.correoInstitucional = correoInstitucional; }
    public int getCursoId() { return cursoId; }
    public void setCursoId(int cursoId) { this.cursoId = cursoId; }
}