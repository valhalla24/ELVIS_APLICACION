package co.edu.usc.elvis.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Donde vive todo lo que NO debe quedar dentro del programa: la base de datos,
 * el archivo config.properties (con la cuenta de correo y su clave cifrada) y
 * la llave de cifrado elvis.key.
 *
 * La carpeta es, en este orden:
 *   1. la propiedad de Java  -Delvis.datos=RUTA
 *   2. la variable de entorno ELVIS_DATOS
 *   3. C:/ElvisData
 *
 * IMPORTANTE: esa carpeta debe tener permisos solo para los profesores y el
 * tecnico (no para las cuentas de los estudiantes); ver el manual.
 */
public final class Configuracion {

    public static final String CARPETA_POR_DEFECTO = "C:/ElvisData";

    private Configuracion() {
    }

    public static File carpetaDatos() {
        String propiedad = System.getProperty("elvis.datos");
        if (propiedad != null && !propiedad.trim().isEmpty()) {
            return new File(propiedad.trim());
        }
        String entorno = System.getenv("ELVIS_DATOS");
        if (entorno != null && !entorno.trim().isEmpty()) {
            return new File(entorno.trim());
        }
        return new File(CARPETA_POR_DEFECTO);
    }

    public static File archivoConfiguracion() {
        return new File(carpetaDatos(), "config.properties");
    }

    /**
     * Llave de cifrado (ver CifradoUtil). Por defecto va junto a los datos;
     * con -Delvis.llave=RUTA o ELVIS_LLAVE puede guardarse en otra carpeta con
     * permisos mas estrictos.
     */
    // ponytail: llave junto a los datos protege copias sueltas de elvis.db / config.properties
    // (respaldos, archivos enviados), no a quien lea toda la carpeta; para eso, moverla con elvis.llave o usar DPAPI.
    public static File archivoLlave() {
        String propiedad = System.getProperty("elvis.llave");
        if (propiedad != null && !propiedad.trim().isEmpty()) {
            return new File(propiedad.trim());
        }
        String entorno = System.getenv("ELVIS_LLAVE");
        if (entorno != null && !entorno.trim().isEmpty()) {
            return new File(entorno.trim());
        }
        return new File(carpetaDatos(), "elvis.key");
    }

    /** Claves de config.properties que se guardan cifradas. */
    static final String[] CLAVES_SECRETAS = {"correo.clave", "correo.oauth.cliente_secreto"};

    /** Igual que texto(), pero descifra el valor si esta cifrado. */
    public static String secreto(Properties propiedades, String clave) {
        return CifradoUtil.descifrar(texto(propiedades, clave, ""));
    }

    /**
     * Si alguien escribio una clave en texto plano en config.properties, la
     * reemplaza en el archivo por su version cifrada (el resto del archivo,
     * comentarios incluidos, queda igual). Se llama al iniciar y antes de cada envio.
     */
    public static synchronized void protegerSecretos() {
        File archivo = archivoConfiguracion();
        if (!archivo.isFile()) {
            return;
        }
        Properties propiedades = cargar();
        try {
            java.util.List<String> lineas = Files.readAllLines(archivo.toPath(), StandardCharsets.UTF_8);
            boolean cambio = false;
            for (int i = 0; i < lineas.size(); i++) {
                String linea = lineas.get(i).trim();
                for (String clave : CLAVES_SECRETAS) {
                    if (linea.matches(java.util.regex.Pattern.quote(clave) + "\\s*[=:].*")) {
                        String valor = texto(propiedades, clave, "");
                        if (!valor.isEmpty() && !CifradoUtil.estaCifrado(valor)) {
                            lineas.set(i, clave + "=" + CifradoUtil.cifrar(valor));
                            cambio = true;
                        }
                    }
                }
            }
            if (cambio) {
                Files.write(archivo.toPath(), lineas, StandardCharsets.UTF_8);
                System.out.println("Se cifraron las claves escritas en texto plano en " + archivo.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("No se pudieron cifrar las claves de " + archivo + ": " + e.getMessage());
        }
    }

    /** Lee el archivo cada vez, asi los cambios se aplican sin cerrar el programa. */
    public static Properties cargar() {
        Properties propiedades = new Properties();
        File archivo = archivoConfiguracion();
        if (archivo.isFile()) {
            try (Reader lector = new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8)) {
                propiedades.load(lector);
            } catch (IOException e) {
                System.err.println("No se pudo leer " + archivo + ": " + e.getMessage());
            }
        }
        return propiedades;
    }

    public static String texto(Properties propiedades, String clave, String porDefecto) {
        String valor = propiedades.getProperty(clave);
        return (valor == null || valor.trim().isEmpty()) ? porDefecto : valor.trim();
    }

    /** Crea config.properties con valores vacios la primera vez, para que solo haya que completarlo. */
    public static void crearPlantillaSiNoExiste() {
        File archivo = archivoConfiguracion();
        if (archivo.exists()) {
            return;
        }
        archivo.getParentFile().mkdirs();
        String plantilla = "# ============================================================\n"
                + "# Servidor ELVIS - configuración\n"
                + "# Edita este archivo con el Bloc de notas. Los cambios del correo\n"
                + "# se aplican en el siguiente envío (no hace falta cerrar el programa).\n"
                + "# Escribe la clave tal cual: al abrir el programa (o al enviar) se\n"
                + "# reemplaza sola por una versión cifrada que empieza por enc:v1:.\n"
                + "# IMPORTANTE: no compartas este archivo ni el archivo elvis.key.\n"
                + "# ============================================================\n"
                + "\n"
                + "# --- Correo que envía las credenciales a los estudiantes ---\n"
                + "# Buzón de Microsoft 365 (smtp.office365.com:587, STARTTLS).\n"
                + "correo.usuario=\n"
                + "correo.remitente=Servidor ELVIS - USC\n"
                + "\n"
                + "# clave  = usuario y contraseña del buzón (requiere \"SMTP autenticado\" activo)\n"
                + "# oauth2 = aplicación registrada en Entra ID, si la cuenta ya no acepta usuario y clave\n"
                + "correo.autenticacion=clave\n"
                + "correo.clave=\n"
                + "\n"
                + "# --- Solo para oauth2 ---\n"
                + "# Id. de directorio (tenant), Id. de aplicación (cliente) y el secreto de cliente.\n"
                + "correo.oauth.tenant=\n"
                + "correo.oauth.cliente_id=\n"
                + "correo.oauth.cliente_secreto=\n";
        try (Writer escritor = new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8)) {
            escritor.write(plantilla);
            System.out.println("Se creó el archivo de configuración: " + archivo.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("No se pudo crear " + archivo + ": " + e.getMessage());
        }
    }
}