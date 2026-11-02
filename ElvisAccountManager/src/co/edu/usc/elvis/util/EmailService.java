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
 * Envia el correo con las credenciales. La cuenta que envia (servidor, puerto,
 * usuario y clave) NO esta en el codigo: se lee de config.properties, en la
 * carpeta de datos, cada vez que se envia.
 */
public class EmailService {

    private static final int TIEMPO_MAXIMO_MS = 15000;

    /** True si config.properties ya tiene el usuario y la clave del correo. */
    public boolean estaConfigurado() {
        Properties cfg = Configuracion.cargar();
        return !Configuracion.texto(cfg, "correo.usuario", "").isEmpty()
                && !Configuracion.texto(cfg, "correo.clave", "").isEmpty();
    }

    public String mensajeSinConfigurar() {
        return "El correo todavía no está configurado. Abre el archivo "
                + Configuracion.archivoConfiguracion().getAbsolutePath()
                + " con el Bloc de notas y completa correo.usuario y correo.clave.";
    }

    public void enviarCredenciales(AsignacionDetalle asignacion) throws MessagingException {
        Properties cfg = Configuracion.cargar();
        final String usuario = Configuracion.texto(cfg, "correo.usuario", "");
        final String clave = Configuracion.texto(cfg, "correo.clave", "");
        if (usuario.isEmpty() || clave.isEmpty()) {
            throw new MessagingException(mensajeSinConfigurar());
        }
        String servidor = Configuracion.texto(cfg, "correo.servidor", "smtp.gmail.com");
        int puerto = 587;
        try {
            puerto = Integer.parseInt(Configuracion.texto(cfg, "correo.puerto", "587"));
        } catch (NumberFormatException e) {
            throw new MessagingException("correo.puerto debe ser un número (por ejemplo 587).");
        }
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
        propiedades.put("mail.smtp.host", servidor);
        propiedades.put("mail.smtp.port", String.valueOf(puerto));
        if (puerto == 465) {
            propiedades.put("mail.smtp.ssl.enable", "true");
        } else {
            propiedades.put("mail.smtp.starttls.enable", "true");
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
        }
    }
}