package co.edu.usc.elvis.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra lo que la aplicacion necesita volver a leer en claro: las contraseñas
 * de las licencias (se envian al estudiante) y la clave del correo.
 * AES-256-GCM con una llave aleatoria guardada en elvis.key (ver Configuracion).
 *
 * Un valor cifrado empieza por "enc:v1:". Lo que no tiene ese prefijo se
 * considera texto plano de una version anterior y se devuelve tal cual, para
 * que las bases y configuraciones viejas sigan funcionando mientras se migran.
 */
public final class CifradoUtil {

    public static final String PREFIJO = "enc:v1:";
    private static final int BYTES_LLAVE = 32;
    private static final int BYTES_IV = 12;
    private static final int BITS_TAG = 128;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private static byte[] llave;
    private static File archivoLlaveCargado;

    private CifradoUtil() {
    }

    public static boolean estaCifrado(String valor) {
        return valor != null && valor.startsWith(PREFIJO);
    }

    public static String cifrar(String texto) {
        if (texto == null || texto.isEmpty() || estaCifrado(texto)) {
            return texto;
        }
        try {
            byte[] iv = new byte[BYTES_IV];
            ALEATORIO.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(llave(), "AES"), new GCMParameterSpec(BITS_TAG, iv));
            byte[] cifrado = c.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] todo = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, todo, 0, iv.length);
            System.arraycopy(cifrado, 0, todo, iv.length, cifrado.length);
            return PREFIJO + Base64.getEncoder().encodeToString(todo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar: " + e.getMessage(), e);
        }
    }

    public static String descifrar(String valor) {
        if (!estaCifrado(valor)) {
            return valor;
        }
        try {
            byte[] todo = Base64.getDecoder().decode(valor.substring(PREFIJO.length()));
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(llave(), "AES"), new GCMParameterSpec(BITS_TAG, todo, 0, BYTES_IV));
            return new String(c.doFinal(todo, BYTES_IV, todo.length - BYTES_IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo descifrar un dato guardado. La llave "
                    + Configuracion.archivoLlave().getAbsolutePath()
                    + " no corresponde a estos datos (¿se reemplazó o se perdió?).", e);
        }
    }

    /**
     * Lee la llave o, si todavia no existe, la crea. Nunca reemplaza una llave
     * existente: si se pierde, los datos cifrados con ella no se pueden recuperar
     * (las licencias se vuelven a cargar desde el Excel y la clave del correo se escribe de nuevo).
     */
    private static synchronized byte[] llave() {
        File archivo = Configuracion.archivoLlave();
        if (llave != null && archivo.equals(archivoLlaveCargado)) {
            return llave;
        }
        try {
            if (!archivo.exists()) {
                archivo.getAbsoluteFile().getParentFile().mkdirs();
                byte[] nueva = new byte[BYTES_LLAVE];
                ALEATORIO.nextBytes(nueva);
                Files.write(archivo.toPath(), Base64.getEncoder().encode(nueva));
                System.out.println("Se creó la llave de cifrado: " + archivo.getAbsolutePath()
                        + " (respáldala junto con la base de datos).");
            }
            byte[] leida = Base64.getDecoder().decode(
                    new String(Files.readAllBytes(archivo.toPath()), StandardCharsets.US_ASCII).trim());
            if (leida.length != BYTES_LLAVE) {
                throw new IllegalStateException("La llave " + archivo + " no es válida.");
            }
            llave = leida;
            archivoLlaveCargado = archivo;
            return llave;
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("No se pudo leer ni crear la llave de cifrado " + archivo + ": " + e.getMessage(), e);
        }
    }
}
