package co.edu.usc.elvis.util;

import co.edu.usc.elvis.model.AsignacionDetalle;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Envia el correo con las credenciales. La cuenta que envia NO esta en el
 * codigo: se lee de config.properties, en la carpeta de datos, cada vez que se envia.
 *
 * Se envia por Microsoft 365 (smtp.office365.com:587 con STARTTLS), con la clave
 * del buzon o, si la cuenta ya no acepta usuario y clave, con
 * correo.autenticacion=oauth2 y una aplicacion registrada en Entra ID (ver el manual).
 */
public class EmailService {

    private static final String SERVIDOR = "smtp.office365.com";
    private static final int PUERTO = 587;
    private static final int TIEMPO_MAXIMO_MS = 15000;

    /** Token de Microsoft reutilizado entre envios hasta poco antes de vencer (dura ~1 h). */
    private static String tokenOauth;
    private static long tokenVenceMs;
    private static String tokenClave;

    /** True si config.properties ya tiene lo necesario para enviar. */
    public boolean estaConfigurado() {
        Properties cfg = Configuracion.cargar();
        if (Configuracion.texto(cfg, "correo.usuario", "").isEmpty()) {
            return false;
        }
        if (usaOauth(cfg)) {
            return !Configuracion.texto(cfg, "correo.oauth.tenant", "").isEmpty()
                    && !Configuracion.texto(cfg, "correo.oauth.cliente_id", "").isEmpty()
                    && !Configuracion.texto(cfg, "correo.oauth.cliente_secreto", "").isEmpty();
        }
        return !Configuracion.texto(cfg, "correo.clave", "").isEmpty();
    }

    public String mensajeSinConfigurar() {
        return "El correo todavía no está configurado. Abre el archivo "
                + Configuracion.archivoConfiguracion().getAbsolutePath()
                + " con el Bloc de notas y completa correo.usuario y correo.clave"
                + " (o, con correo.autenticacion=oauth2, los datos correo.oauth.*).";
    }

    private static boolean usaOauth(Properties cfg) {
        return "oauth2".equalsIgnoreCase(Configuracion.texto(cfg, "correo.autenticacion", "clave"));
    }

    public void enviarCredenciales(AsignacionDetalle asignacion) throws MessagingException {
        Properties cfg = Configuracion.cargar();
        if (!estaConfigurado()) {
            throw new MessagingException(mensajeSinConfigurar());
        }
        final String usuario = Configuracion.texto(cfg, "correo.usuario", "");
        final boolean oauth = usaOauth(cfg);
        final String clave = oauth ? obtenerTokenMicrosoft(cfg) : Configuracion.texto(cfg, "correo.clave", "");
        String nombreRemitente = Configuracion.texto(cfg, "correo.remitente", "Servidor ELVIS - USC");

        String asunto = "Acceso al Servidor ELVIS - " + asignacion.getCursoNombre();

        String cuerpo = "Hola " + asignacion.getEstudianteNombre() + ",\n\n"
                + "Se te ha asignado acceso al Servidor ELVIS para el curso "
                + asignacion.getCursoNombre() + " (" + asignacion.getCursoCodigo() + ").\n\n"
                + "Horario de acceso: " + asignacion.getDiaFranja() + ", " + asignacion.getHoraInicio()
                + " - " + asignacion.getHoraFin() + "\n\n"
                + "Credenciales de acceso:\n"
                + "  Usuario: " + asignacion.getLicenciaUsuario() + "\n"
                + "  Contraseña: " + asignacion.getLicenciaContrasena() + "\n"
                + "  IP del servidor: " + asignacion.getLicenciaIp() + "\n\n"
                + "Instrucciones:\n"
                + "  1. Conéctate por escritorio remoto a la IP indicada arriba.\n"
                + "  2. Ingresa con el usuario y la contraseña de arriba.\n"
                + "  3. Usa el servidor únicamente durante tu horario de clase asignado.\n\n"
                + "Este acceso es personal e intransferible.\n\n"
                + "Dirección de Tecnologías y Servicios de Información (DTSI)\n"
                + "Universidad Santiago de Cali";

        Properties propiedades = new Properties();
        propiedades.put("mail.smtp.auth", "true");
        propiedades.put("mail.smtp.host", SERVIDOR);
        propiedades.put("mail.smtp.port", String.valueOf(PUERTO));
        propiedades.put("mail.smtp.starttls.enable", "true");
        propiedades.put("mail.smtp.starttls.required", "true");
        propiedades.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        if (oauth) {
            // XOAUTH2: javax.mail manda el token de Microsoft en lugar de la clave.
            propiedades.put("mail.smtp.auth.mechanisms", "XOAUTH2");
        }
        // Sin tiempos maximos, un servidor que no responde dejaria colgado el envio.
        propiedades.put("mail.smtp.connectiontimeout", String.valueOf(TIEMPO_MAXIMO_MS));
        propiedades.put("mail.smtp.timeout", String.valueOf(TIEMPO_MAXIMO_MS));
        propiedades.put("mail.smtp.writetimeout", String.valueOf(TIEMPO_MAXIMO_MS));

        Session sesion = Session.getInstance(propiedades, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, clave);
            }
        });

        try {
            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(usuario, nombreRemitente));
            mensaje.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(asignacion.getEstudianteCorreo()));
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            Transport.send(mensaje);
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Error al construir el remitente: " + e.getMessage(), e);
        } catch (AuthenticationFailedException e) {
            throw new MessagingException(explicarRechazo(oauth, e.getMessage()), e);
        }
    }

    /** Traduce el rechazo de Microsoft 365 a lo que hay que corregir en config.properties. */
    private static String explicarRechazo(boolean oauth, String detalle) {
        String d = detalle == null ? "" : detalle;
        if (!oauth) {
            return "Microsoft 365 rechazó el usuario y la clave. Revisa que correo.clave sea la clave vigente del buzón; "
                    + "si lo es, la cuenta no permite este tipo de acceso: activa \"SMTP autenticado\" en el buzón "
                    + "o usa correo.autenticacion=oauth2. Detalle: " + d;
        }
        return "Microsoft 365 rechazó el acceso con OAuth. Revisa que SMTP autenticado esté activo en el buzón "
                + "y que la aplicación tenga permiso sobre él (manual, paso de Exchange). Detalle: " + d;
    }

    /** Pide (o reutiliza) un token de Microsoft con el flujo client credentials. */
    private static synchronized String obtenerTokenMicrosoft(Properties cfg) throws MessagingException {
        String tenant = Configuracion.texto(cfg, "correo.oauth.tenant", "");
        String clienteId = Configuracion.texto(cfg, "correo.oauth.cliente_id", "");
        String secreto = Configuracion.texto(cfg, "correo.oauth.cliente_secreto", "");
        String clave = tenant + "|" + clienteId + "|" + secreto;
        if (tokenOauth != null && clave.equals(tokenClave) && System.currentTimeMillis() < tokenVenceMs) {
            return tokenOauth;
        }
        String cuerpo = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(clienteId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(secreto, StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode("https://outlook.office365.com/.default", StandardCharsets.UTF_8);
        String respuesta;
        try {
            HttpURLConnection con = (HttpURLConnection) URI.create(
                    "https://login.microsoftonline.com/" + URLEncoder.encode(tenant, StandardCharsets.UTF_8)
                    + "/oauth2/v2.0/token").toURL().openConnection();
            con.setConnectTimeout(TIEMPO_MAXIMO_MS);
            con.setReadTimeout(TIEMPO_MAXIMO_MS);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (OutputStream salida = con.getOutputStream()) {
                salida.write(cuerpo.getBytes(StandardCharsets.UTF_8));
            }
            // Con error (400/401) Microsoft explica el motivo en el cuerpo de getErrorStream().
            InputStream entrada = con.getResponseCode() < 400 ? con.getInputStream() : con.getErrorStream();
            try (InputStream in = entrada) {
                respuesta = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new MessagingException("No se pudo contactar a Microsoft para pedir el token: " + e.getMessage(), e);
        }
        String token = campoJson(respuesta, "access_token");
        if (token == null) {
            String error = campoJson(respuesta, "error_description");
            throw new MessagingException("Microsoft no entregó el token. Revisa correo.oauth.tenant, cliente_id y "
                    + "cliente_secreto. Detalle: " + (error != null ? error : respuesta));
        }
        long segundos = 3600;
        Matcher m = Pattern.compile("\"expires_in\"\\s*:\\s*\"?(\\d+)").matcher(respuesta);
        if (m.find()) {
            segundos = Long.parseLong(m.group(1));
        }
        tokenOauth = token;
        tokenClave = clave;
        tokenVenceMs = System.currentTimeMillis() + (segundos - 120) * 1000;
        return token;
    }

    // ponytail: lector minimo de un campo de texto JSON; la respuesta de Microsoft es plana y no trae comillas escapadas en estos campos.
    private static String campoJson(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }
}