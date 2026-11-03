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
import java.util.Properties;

/**
 * Donde vive todo lo que NO debe quedar dentro del programa: la base de datos
 * y el archivo config.properties (con la cuenta de correo y su clave).
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
                + "# IMPORTANTE: aquí va la clave del correo; no compartas este archivo.\n"
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