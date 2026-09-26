package co.edu.usc.elvis.util;

import co.edu.usc.elvis.model.AsignacionDetalle;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Arma y envia el correo de notificacion de credenciales a un estudiante,
 * usando JavaMail (la libreria base de Java para SMTP; un solo jar, sin
 * modulos adicionales que configurar).
 *
 * IMPORTANTE: los valores de SMTP_* de aqui abajo son de ejemplo (Gmail).
 * Hay que reemplazarlos por la cuenta real que va a enviar los correos:
 *  - SMTP_USUARIO: la direccion de correo que enviara (aparece como remitente)
 *  - SMTP_CLAVE: su contraseña de aplicacion (NO la contraseña normal de la cuenta;
 *    Gmail exige generar una "contraseña de aplicacion" en la configuracion de seguridad)
 *  - Si en vez de Gmail usan un correo institucional con su propio servidor SMTP,
 *    SMTP_HOST y SMTP_PUERTO cambian (DTSI deberia poder dar esos datos).
 */
public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PUERTO = 587;
    private static final String SMTP_USUARIO = "reemplazar@gmail.com";
    private static final String SMTP_CLAVE = "reemplazar_contrasena_de_aplicacion";
    private static final String REMITENTE_NOMBRE = "Servidor ELVIS - USC";

    public void enviarCredenciales(AsignacionDetalle asignacion) throws MessagingException {
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
        propiedades.put("mail.smtp.starttls.enable", "true");
        propiedades.put("mail.smtp.host", SMTP_HOST);
        propiedades.put("mail.smtp.port", String.valueOf(SMTP_PUERTO));

        Session sesion = Session.getInstance(propiedades, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USUARIO, SMTP_CLAVE);
            }
        });

        try {
            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(SMTP_USUARIO, REMITENTE_NOMBRE));
            mensaje.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(asignacion.getEstudianteCorreo()));
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            Transport.send(mensaje);
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Error al construir el remitente: " + e.getMessage(), e);
        }
    }
}