package co.edu.usc.elvis.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Contraseñas de los usuarios del sistema (administrador y profesores).
 *
 * Se guardan con PBKDF2-HMAC-SHA256 (600.000 iteraciones, recomendacion de
 * OWASP), con el formato "pbkdf2$ITERACIONES$HASH". Las cuentas creadas por
 * versiones anteriores tienen un SHA-256 simple: se siguen aceptando y al
 * iniciar sesion se convierten a PBKDF2 (ver LoginController).
 */
public class SeguridadUtil {

    public static final String CLAVE_INICIAL_ADMIN = "admin123";
    public static final int LARGO_MINIMO = 8;
    static final int ITERACIONES = 600_000;
    private static final String PREFIJO = "pbkdf2$";

    private SeguridadUtil() {
    }

    public static String generarSal() {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return Base64.getEncoder().encodeToString(sal);
    }

    /** Hash para guardar una contraseña nueva. */
    public static String hashContrasena(String contrasena, String sal) {
        return PREFIJO + ITERACIONES + "$" + pbkdf2(contrasena, sal, ITERACIONES);
    }

    /** Compara en tiempo constante; acepta el formato PBKDF2 y el SHA-256 de versiones anteriores. */
    public static boolean verificar(String contrasena, String sal, String hashGuardado) {
        if (contrasena == null || sal == null || hashGuardado == null) {
            return false;
        }
        String calculado;
        if (hashGuardado.startsWith(PREFIJO)) {
            String[] partes = hashGuardado.split("\\$");
            if (partes.length != 3) {
                return false;
            }
            calculado = PREFIJO + partes[1] + "$" + pbkdf2(contrasena, sal, Integer.parseInt(partes[1]));
        } else {
            calculado = sha256Antiguo(contrasena, sal);
        }
        return MessageDigest.isEqual(calculado.getBytes(StandardCharsets.UTF_8),
                hashGuardado.getBytes(StandardCharsets.UTF_8));
    }

    /** True si el hash es del formato antiguo o con menos iteraciones de las actuales. */
    public static boolean necesitaActualizar(String hashGuardado) {
        if (hashGuardado == null || !hashGuardado.startsWith(PREFIJO)) {
            return true;
        }
        String[] partes = hashGuardado.split("\\$");
        return partes.length != 3 || Integer.parseInt(partes[1]) < ITERACIONES;
    }

    /** Devuelve el motivo por el que la nueva contraseña no sirve, o null si sirve. */
    public static String validarNuevaContrasena(String nueva, String confirmacion, String actual) {
        if (nueva == null || nueva.length() < LARGO_MINIMO) {
            return "La nueva contraseña debe tener al menos " + LARGO_MINIMO + " caracteres.";
        }
        if (!nueva.equals(confirmacion)) {
            return "La confirmación no coincide con la nueva contraseña.";
        }
        if (nueva.equals(actual)) {
            return "La nueva contraseña debe ser distinta de la actual.";
        }
        if (nueva.equalsIgnoreCase(CLAVE_INICIAL_ADMIN)) {
            return "No se puede usar la contraseña inicial del sistema.";
        }
        return null;
    }

    private static String pbkdf2(String contrasena, String sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(), Base64.getDecoder().decode(sal), iteraciones, 256);
        try {
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no disponible", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static String sha256Antiguo(String contrasena, String sal) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.getDecoder().decode(sal));
            byte[] hash = md.digest(contrasena.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
