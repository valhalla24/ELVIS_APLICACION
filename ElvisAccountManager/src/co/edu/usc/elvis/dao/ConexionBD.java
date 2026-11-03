package co.edu.usc.elvis.dao;

import co.edu.usc.elvis.util.Configuracion;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Una sola conexion a SQLite, abierta una vez y reutilizada en todo el programa.
 *
 * Antes cada consulta abria y cerraba su propia conexion (y repetia los PRAGMA),
 * lo que hacia lenta cada pantalla. Los DAO siguen escribiendo
 * try (Connection conn = ConexionBD.obtenerConexion(); ...), pero lo que reciben
 * es una "fachada" cuyo close() no hace nada: la conexion real se cierra sola al
 * terminar el programa.
 *
 * La base vive en la carpeta de datos (ver Configuracion), no dentro del programa.
 */
public class ConexionBD {

    /** Carpeta que usaban las versiones anteriores; si hay una base ahi, se copia a la carpeta nueva. */
    private static final String CARPETA_ANTIGUA = "C:/Users/Public/ElvisAccountManager";

    private static Connection conexionReal;
    private static Connection fachada;

    private ConexionBD() {
    }

    public static synchronized Connection obtenerConexion() throws SQLException {
        if (conexionReal == null || conexionReal.isClosed()) {
            File carpeta = Configuracion.carpetaDatos();
            carpeta.mkdirs();
            migrarBaseAntiguaSiHaceFalta(carpeta);

            conexionReal = DriverManager.getConnection("jdbc:sqlite:" + rutaBase(carpeta));
            try (Statement stmt = conexionReal.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");
                // Si otro profesor esta escribiendo en ese instante, esperar hasta 5 s en vez de fallar.
                stmt.execute("PRAGMA busy_timeout = 5000;");
            }
            final Connection paraCerrar = conexionReal;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    paraCerrar.close();
                } catch (SQLException e) {
                    // al salir del programa no hay nada mas que hacer
                }
            }));
            fachada = crearFachada(conexionReal);
        }
        return fachada;
    }

    private static String rutaBase(File carpeta) {
        return new File(carpeta, "elvis.db").getPath().replace('\\', '/');
    }

    private static Connection crearFachada(final Connection real) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, args) -> {
                    if (metodo.getName().equals("close")) {
                        return null; // se ignora: la conexion se reutiliza
                    }
                    try {
                        return metodo.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    /**
     * Si la carpeta nueva todavia no tiene base de datos y la carpeta antigua si,
     * la copia (VACUUM INTO hace una copia consistente, incluso con modo WAL).
     * La base antigua NO se borra: hay que eliminarla a mano cuando se confirme que todo funciona.
     */
    private static void migrarBaseAntiguaSiHaceFalta(File carpetaNueva) {
        File nueva = new File(carpetaNueva, "elvis.db");
        File antigua = new File(CARPETA_ANTIGUA, "elvis.db");
        if (nueva.exists() || !antigua.isFile()) {
            return;
        }
        try (Connection origen = DriverManager.getConnection("jdbc:sqlite:" + antigua.getPath().replace('\\', '/'));
             Statement stmt = origen.createStatement()) {
            stmt.execute("VACUUM INTO '" + rutaBase(carpetaNueva).replace("'", "''") + "'");
            System.out.println("Se copió la base de datos antigua a " + nueva.getAbsolutePath()
                    + ". Cuando confirmes que todo funciona, borra la carpeta " + CARPETA_ANTIGUA + ".");
        } catch (SQLException e) {
            System.err.println("No se pudo copiar la base de datos antigua: " + e.getMessage());
        }
    }

    public static void inicializarBaseDeDatos() {
        Configuracion.crearPlantillaSiNoExiste();

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
                + "usuario_id INTEGER,"
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

        String sqlUsuarioSistema = "CREATE TABLE IF NOT EXISTS usuario_sistema ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "nombre_completo TEXT NOT NULL,"
                + "nombre_usuario TEXT NOT NULL UNIQUE,"
                + "contrasena_hash TEXT NOT NULL,"
                + "sal TEXT NOT NULL,"
                + "rol TEXT NOT NULL DEFAULT 'PROFESOR',"
                + "debe_cambiar_clave INTEGER NOT NULL DEFAULT 0,"
                + "fecha_creacion TEXT NOT NULL DEFAULT (datetime('now'))"
                + ");";

        try (Connection conn = obtenerConexion();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlCurso);
            stmt.execute(sqlEstudiante);
            stmt.execute(sqlLicencia);
            stmt.execute(sqlAsignacion);
            stmt.execute(sqlLog);
            stmt.execute(sqlUsuarioSistema);
            System.out.println("Base de datos inicializada correctamente.");
        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
        }

        asegurarColumnaDuenoEnCurso();
        asegurarColumna("usuario_sistema", "debe_cambiar_clave", "INTEGER NOT NULL DEFAULT 0");
        sembrarUsuarioAdminSiEstaVacio();
        cifrarLicenciasEnTextoPlano();
        co.edu.usc.elvis.util.Configuracion.protegerSecretos();
    }

    /** Agrega una columna a una tabla de una base anterior, sin borrar nada. */
    private static void asegurarColumna(String tabla, String columna, String definicion) {
        try (Connection conn = obtenerConexion();
             Statement stmt = conn.createStatement()) {
            boolean existe = false;
            try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tabla + ")")) {
                while (rs.next()) {
                    existe |= columna.equalsIgnoreCase(rs.getString("name"));
                }
            }
            if (!existe) {
                stmt.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
                System.out.println("Se agregó la columna " + columna + " a la tabla " + tabla + ".");
            }
        } catch (SQLException e) {
            System.err.println("Error al agregar la columna " + columna + ": " + e.getMessage());
        }
    }

    /**
     * Las versiones anteriores guardaban las contraseñas de las licencias en
     * texto plano: se cifran una sola vez al abrir el programa.
     */
    private static void cifrarLicenciasEnTextoPlano() {
        String sql = "SELECT id, contrasena FROM licencia WHERE contrasena NOT LIKE '"
                + co.edu.usc.elvis.util.CifradoUtil.PREFIJO + "%'";
        int cifradas = 0;
        try (Connection conn = obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql);
             PreparedStatement ps = conn.prepareStatement("UPDATE licencia SET contrasena = ? WHERE id = ?")) {
            while (rs.next()) {
                ps.setString(1, co.edu.usc.elvis.util.CifradoUtil.cifrar(rs.getString("contrasena")));
                ps.setInt(2, rs.getInt("id"));
                ps.addBatch();
                cifradas++;
            }
            if (cifradas > 0) {
                ps.executeBatch();
                System.out.println("Se cifraron " + cifradas + " contraseña(s) de licencias guardadas en texto plano.");
            }
        } catch (SQLException e) {
            System.err.println("Error al cifrar las contraseñas de las licencias: " + e.getMessage());
        }
    }

    /**
     * Las bases de datos creadas antes de existir el "dueño de curso" no
     * tienen la columna usuario_id en la tabla curso. Se agrega sola, sin
     * borrar nada: esos cursos quedan "sin dueño" hasta que el administrador
     * los asigne a alguien.
     */
    private static void asegurarColumnaDuenoEnCurso() {
        boolean existe = false;
        try (Connection conn = obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(curso)")) {
            while (rs.next()) {
                if ("usuario_id".equalsIgnoreCase(rs.getString("name"))) {
                    existe = true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al revisar la tabla curso: " + e.getMessage());
            return;
        }

        if (!existe) {
            try (Connection conn = obtenerConexion();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE curso ADD COLUMN usuario_id INTEGER");
                System.out.println("Se agregó la columna usuario_id (dueño del curso) a la tabla curso.");
            } catch (SQLException e) {
                System.err.println("Error al agregar la columna usuario_id: " + e.getMessage());
            }
        }
    }

    private static void sembrarUsuarioAdminSiEstaVacio() {
        String sqlConteo = "SELECT COUNT(*) AS total FROM usuario_sistema";
        try (Connection conn = obtenerConexion();
             Statement stmtConteo = conn.createStatement();
             ResultSet rs = stmtConteo.executeQuery(sqlConteo)) {

            int total = rs.next() ? rs.getInt("total") : 0;
            if (total == 0) {
                String sal = co.edu.usc.elvis.util.SeguridadUtil.generarSal();
                String hash = co.edu.usc.elvis.util.SeguridadUtil.hashContrasena(
                        co.edu.usc.elvis.util.SeguridadUtil.CLAVE_INICIAL_ADMIN, sal);
                String sqlInsertar = "INSERT INTO usuario_sistema "
                        + "(nombre_completo, nombre_usuario, contrasena_hash, sal, rol, debe_cambiar_clave) "
                        + "VALUES (?, ?, ?, ?, 'ADMINISTRADOR', 1)";
                try (PreparedStatement ps = conn.prepareStatement(sqlInsertar)) {
                    ps.setString(1, "Administrador del Sistema");
                    ps.setString(2, "admin");
                    ps.setString(3, hash);
                    ps.setString(4, sal);
                    ps.executeUpdate();
                }
                System.out.println("Usuario administrador inicial creado -> usuario: admin / contraseña: admin123 "
                        + "(el programa pedirá cambiarla en el primer inicio de sesión)");
            }
        } catch (SQLException e) {
            System.err.println("Error al sembrar usuario administrador inicial: " + e.getMessage());
        }
    }
}