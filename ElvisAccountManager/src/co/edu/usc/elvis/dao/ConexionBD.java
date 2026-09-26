package co.edu.usc.elvis.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ConexionBD {

    private static final String URL_BD = "jdbc:sqlite:elvis.db";

    private static Connection conexion;

    private ConexionBD() {
    }

    public static Connection obtenerConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = DriverManager.getConnection(URL_BD);
            try (Statement stmt = conexion.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return conexion;
    }

    public static void inicializarBaseDeDatos() {
        String sqlCurso = "CREATE TABLE IF NOT EXISTS curso ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "nombre_materia TEXT NOT NULL,"
                + "codigo_curso TEXT NOT NULL UNIQUE,"
                + "semestre_lectivo TEXT NOT NULL,"
                + "docente_responsable TEXT NOT NULL,"
                + "dia_clase TEXT NOT NULL,"
                + "hora_inicio TEXT NOT NULL,"
                + "hora_fin TEXT NOT NULL,"
                + "licencias_requeridas INTEGER NOT NULL,"
                + "fecha_registro TEXT NOT NULL DEFAULT (datetime('now'))"
                + ");";

        String sqlEstudiante = "CREATE TABLE IF NOT EXISTS estudiante ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "nombre_completo TEXT NOT NULL,"
                + "codigo_estudiantil TEXT NOT NULL UNIQUE,"
                + "correo_institucional TEXT NOT NULL UNIQUE,"
                + "curso_id INTEGER,"
                + "FOREIGN KEY (curso_id) REFERENCES curso(id)"
                + ");";

        String sqlLicencia = "CREATE TABLE IF NOT EXISTS licencia ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "usuario TEXT NOT NULL UNIQUE,"
                + "contrasena TEXT NOT NULL,"
                + "ip_asignada TEXT,"
                + "estado TEXT NOT NULL DEFAULT 'Disponible',"
                + "fecha_asignacion TEXT,"
                + "vigencia_hasta TEXT"
                + ");";

                String sqlAsignacion = "CREATE TABLE IF NOT EXISTS asignacion ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "licencia_id INTEGER NOT NULL,"
                + "curso_id INTEGER NOT NULL,"
                + "estudiante_id INTEGER,"
                + "dia_franja TEXT NOT NULL,"
                + "hora_inicio TEXT NOT NULL,"
                + "hora_fin TEXT NOT NULL,"
                + "notificado INTEGER NOT NULL DEFAULT 0,"
                + "fecha_asignacion TEXT NOT NULL DEFAULT (datetime('now')),"
                + "FOREIGN KEY (licencia_id) REFERENCES licencia(id),"
                + "FOREIGN KEY (curso_id) REFERENCES curso(id),"
                + "FOREIGN KEY (estudiante_id) REFERENCES estudiante(id)"
                + ");";

        String sqlLog = "CREATE TABLE IF NOT EXISTS log_auditoria ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "accion TEXT NOT NULL,"
                + "detalle TEXT,"
                + "usuario TEXT,"
                + "fecha_hora TEXT NOT NULL DEFAULT (datetime('now'))"
                + ");";

        try (Connection conn = obtenerConexion();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlCurso);
            stmt.execute(sqlEstudiante);
            stmt.execute(sqlLicencia);
            stmt.execute(sqlAsignacion);
            stmt.execute(sqlLog);
            System.out.println("Base de datos inicializada correctamente.");
        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }
}