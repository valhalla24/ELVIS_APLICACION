import co.edu.usc.elvis.dao.AsignacionDAO;
import co.edu.usc.elvis.dao.ConexionBD;
import co.edu.usc.elvis.dao.LicenciaDAO;
import co.edu.usc.elvis.dao.UsuarioDAO;
import co.edu.usc.elvis.model.Usuario;
import co.edu.usc.elvis.util.CifradoUtil;
import co.edu.usc.elvis.util.Configuracion;
import co.edu.usc.elvis.util.SeguridadUtil;
import co.edu.usc.elvis.util.SesionActual;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Pruebas de las correcciones de seguridad (cifrado, PBKDF2 y cambio obligatorio de clave). */
public class PruebasSeguridad {

    static void caso(String id, String desc, boolean ok, String det) { PruebasElvis.caso(id, desc, ok, det); }

    static List<String> columna(String sql) throws Exception {
        List<String> r = new ArrayList<>();
        Connection c = ConexionBD.obtenerConexion();
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) r.add(rs.getString(1));
        }
        return r;
    }

    static void sql(String sql, Object... p) throws Exception {
        try (PreparedStatement ps = ConexionBD.obtenerConexion().prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
            ps.executeUpdate();
        }
    }

    static String sha256Antiguo(String clave, String sal) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(Base64.getDecoder().decode(sal));
        return Base64.getEncoder().encodeToString(md.digest(clave.getBytes(StandardCharsets.UTF_8)));
    }

    // ---------------------------------------------------------------- login real con el dialogo
    static Stage escenario;
    static Object login;

    static void abrirLogin() throws Exception {
        PruebasElvis.fx(() -> {
            FXMLLoader l = new FXMLLoader(PruebasElvis.class.getResource("/co/edu/usc/elvis/view/Login.fxml"));
            Parent raiz = l.load();
            login = l.getController();
            if (escenario == null) escenario = new Stage();
            escenario.setScene(new Scene(raiz, 900, 600));
            escenario.show();
            return null;
        });
    }

    /** Respuestas al dialogo de cambio de clave, en orden: {nueva, confirmacion} o null = Cancelar. */
    static List<String[]> respuestas = new ArrayList<>();
    static List<String> erroresVistos = new ArrayList<>();
    static int dialogosVistos = 0;

    static void atenderDialogo() {
        Platform.runLater(() -> {
            DialogPane pane = null;
            for (Window w : Window.getWindows()) {
                if (w.isShowing() && w.getScene() != null && w.getScene().getRoot() instanceof DialogPane) pane = (DialogPane) w.getScene().getRoot();
            }
            if (pane == null) { atenderDialogo(); return; }
            dialogosVistos++;
            List<PasswordField> campos = new ArrayList<>();
            Label error = null;
            for (Node n : ((javafx.scene.layout.GridPane) pane.getContent()).getChildren()) {
                if (n instanceof PasswordField) campos.add((PasswordField) n);
                if (n instanceof Label && ((Label) n).getStyleClass().contains("mensaje-error")) error = (Label) n;
            }
            Window ventana = pane.getScene().getWindow();   // se toma antes: al cerrarse, el panel pierde su escena
            while (!respuestas.isEmpty()) {
                String[] r = respuestas.remove(0);
                if (r == null) { ((Button) pane.lookupButton(ButtonType.CANCEL)).fire(); return; }
                campos.get(0).setText(r[0]);
                campos.get(1).setText(r[1]);
                ((Button) pane.lookupButton(ButtonType.OK)).fire();
                if (ventana.isShowing()) erroresVistos.add(error.getText());
                else return;
            }
        });
    }

    /** Escribe usuario y clave y pulsa Ingresar (el metodo real del controlador). */
    static String ingresar(String usuario, String clave, boolean esperarDialogo) throws Exception {
        if (esperarDialogo) atenderDialogo();
        CompletableFuture<String> f = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                Class<?> k = login.getClass();
                java.lang.reflect.Field cu = k.getDeclaredField("campoUsuario"); cu.setAccessible(true);
                java.lang.reflect.Field cc = k.getDeclaredField("campoContrasena"); cc.setAccessible(true);
                java.lang.reflect.Field ce = k.getDeclaredField("etiquetaError"); ce.setAccessible(true);
                ((TextField) cu.get(login)).setText(usuario);
                ((PasswordField) cc.get(login)).setText(clave);
                Method m = k.getDeclaredMethod("iniciarSesion"); m.setAccessible(true);
                m.invoke(login);
                f.complete(((Label) ce.get(login)).getText());
            } catch (Throwable t) { f.completeExceptionally(t); }
        });
        return f.get(60, TimeUnit.SECONDS);
    }

    static void ejecutar(File salida) throws Exception {
        LicenciaDAO ldao = new LicenciaDAO();
        UsuarioDAO udao = new UsuarioDAO();

        // ------------------------------------------------ licencias cifradas
        List<String> guardadas = columna("SELECT contrasena FROM licencia");
        boolean todasCifradas = !guardadas.isEmpty() && guardadas.stream().allMatch(v -> v.startsWith("enc:v1:") && !v.contains("Clave-"));
        caso("SEG-01", "Las contraseñas de licencias se guardan cifradas en la base (" + guardadas.size() + " filas)", todasCifradas, guardadas.isEmpty() ? "vacío" : guardadas.get(0));
        String lic01 = ldao.obtenerTodas().get(0).getContrasena();
        caso("SEG-02", "La aplicación las lee descifradas (lic01 = Clave-01)", "Clave-01".equals(lic01), lic01);
        String paraCorreo = new AsignacionDAO().obtenerTodasConDetalle().stream().filter(d -> "lic03".equals(d.getLicenciaUsuario())).findFirst().get().getLicenciaContrasena();
        caso("SEG-03", "El correo recibe la contraseña descifrada", "Clave-03".equals(paraCorreo), paraCorreo);
        String c1 = CifradoUtil.cifrar("misma"), c2 = CifradoUtil.cifrar("misma");
        caso("SEG-04", "El mismo texto cifrado dos veces da resultados distintos (IV aleatorio)", !c1.equals(c2) && "misma".equals(CifradoUtil.descifrar(c2)), null);
        byte[] b = Base64.getDecoder().decode(c1.substring(7)); b[b.length - 1] ^= 1;
        boolean detecta = false;
        try { CifradoUtil.descifrar("enc:v1:" + Base64.getEncoder().encodeToString(b)); } catch (IllegalStateException e) { detecta = true; }
        caso("SEG-05", "Un valor cifrado alterado se rechaza (GCM)", detecta, null);
        sql("INSERT INTO licencia (usuario, contrasena, ip_asignada, estado) VALUES ('vieja', 'TextoPlano-1', '10.0.0.9', 'Disponible')");
        ConexionBD.inicializarBaseDeDatos();
        String vieja = columna("SELECT contrasena FROM licencia WHERE usuario = 'vieja'").get(0);
        String viejaLeida = ldao.obtenerTodas().stream().filter(l -> l.getUsuario().equals("vieja")).findFirst().get().getContrasena();
        caso("SEG-06", "Migración: una licencia en texto plano de una base anterior se cifra al abrir", vieja.startsWith("enc:v1:") && "TextoPlano-1".equals(viejaLeida), vieja);
        File llave = Configuracion.archivoLlave();
        caso("SEG-07", "La llave se crea en la carpeta de datos (32 bytes)", llave.isFile() && Base64.getDecoder().decode(Files.readString(llave.toPath()).trim()).length == 32, llave.getPath());

        // ------------------------------------------------ config.properties
        File cfg = Configuracion.archivoConfiguracion();
        List<String> lineas = new ArrayList<>(Files.readAllLines(cfg.toPath(), StandardCharsets.UTF_8));
        lineas.replaceAll(l -> l.startsWith("correo.clave=") ? "correo.clave=Mi:Clave=Secreta#1"
                : l.startsWith("correo.oauth.cliente_secreto=") ? "correo.oauth.cliente_secreto=abc~123" : l);
        Files.write(cfg.toPath(), lineas, StandardCharsets.UTF_8);
        int comentariosAntes = (int) lineas.stream().filter(l -> l.startsWith("#")).count();
        Configuracion.protegerSecretos();
        String texto = Files.readString(cfg.toPath(), StandardCharsets.UTF_8);
        java.util.Properties p = Configuracion.cargar();
        boolean cifradoCfg = texto.contains("correo.clave=enc:v1:") && texto.contains("correo.oauth.cliente_secreto=enc:v1:")
                && !texto.contains("Secreta#1") && !texto.contains("abc~123");
        boolean leeBien = "Mi:Clave=Secreta#1".equals(Configuracion.secreto(p, "correo.clave")) && "abc~123".equals(Configuracion.secreto(p, "correo.oauth.cliente_secreto"));
        int comentariosDespues = (int) Files.readAllLines(cfg.toPath()).stream().filter(l -> l.startsWith("#")).count();
        caso("SEG-08", "config.properties: la clave del correo y el secreto OAuth escritos en claro se cifran solos", cifradoCfg, null);
        caso("SEG-09", "config.properties: se leen descifrados (con : = #) y los comentarios se conservan", leeBien && comentariosAntes == comentariosDespues, comentariosAntes + "/" + comentariosDespues);
        Configuracion.protegerSecretos();
        caso("SEG-10", "Cifrar la configuración dos veces no la vuelve a cambiar", texto.equals(Files.readString(cfg.toPath(), StandardCharsets.UTF_8)), null);

        // ------------------------------------------------ contrasenas de usuarios
        Usuario a = udao.buscarPorNombreUsuario("admin");
        caso("SEG-11", "Admin inicial: PBKDF2 (600.000 iteraciones) y cambio obligatorio", a.getContrasenaHash().startsWith("pbkdf2$600000$") && a.isDebeCambiarClave(), a.getContrasenaHash().substring(0, 15));
        caso("SEG-12", "PBKDF2 verifica la clave correcta y rechaza una incorrecta",
                SeguridadUtil.verificar("admin123", a.getSal(), a.getContrasenaHash()) && !SeguridadUtil.verificar("admin124", a.getSal(), a.getContrasenaHash()), null);
        udao.crearUsuario("Cuenta Antigua", "antigua", "temporal1", Usuario.ROL_PROFESOR);
        Usuario ant = udao.buscarPorNombreUsuario("antigua");
        sql("UPDATE usuario_sistema SET contrasena_hash = ?, debe_cambiar_clave = 0 WHERE id = ?", sha256Antiguo("vieja-123", ant.getSal()), ant.getId());
        ant = udao.buscarPorNombreUsuario("antigua");
        boolean legado = SeguridadUtil.verificar("vieja-123", ant.getSal(), ant.getContrasenaHash()) && SeguridadUtil.necesitaActualizar(ant.getContrasenaHash());
        caso("SEG-13", "Hash SHA-256 de versiones anteriores: sigue entrando y se marca para actualizar", legado, null);

        Usuario profa = udao.buscarPorNombreUsuario("profa");
        caso("SEG-14", "Usuario creado por el administrador: debe cambiar la clave al entrar", profa.isDebeCambiarClave(), null);
        String[][] invalidas = {{"corta1", "corta1"}, {"distinta-1", "distinta-2"}, {"clave-a1", "clave-a1"}, {"ADMIN123", "ADMIN123"}};
        boolean todasRechazadas = true;
        for (String[] v : invalidas) todasRechazadas &= SeguridadUtil.validarNuevaContrasena(v[0], v[1], "clave-a1") != null;
        caso("SEG-15", "Nueva clave: se rechazan corta, no coincidente, igual a la actual y admin123; se acepta una válida",
                todasRechazadas && SeguridadUtil.validarNuevaContrasena("Nueva-Clave-9", "Nueva-Clave-9", "clave-a1") == null, null);

        // ------------------------------------------------ login real (pantalla + dialogo)
        abrirLogin();
        SesionActual.cerrarSesion();
        String e1 = ingresar("admin", "malaclave", false);
        caso("SEG-16", "Login con clave incorrecta: no entra", SesionActual.obtenerUsuario() == null && e1.contains("incorrectos"), e1);

        respuestas.clear(); erroresVistos.clear(); dialogosVistos = 0;
        respuestas.add(null);   // Cancelar
        String e2 = ingresar("admin", "admin123", true);
        caso("SEG-17", "admin/admin123: aparece el diálogo y, si se cancela, no entra", dialogosVistos == 1 && SesionActual.obtenerUsuario() == null && e2.contains("Debes cambiar"), e2);

        respuestas.clear(); erroresVistos.clear(); dialogosVistos = 0;
        respuestas.add(new String[]{"corta", "corta"});
        respuestas.add(new String[]{"admin123", "admin123"});
        respuestas.add(new String[]{"Elvis-Admin-2026", "Elvis-Admin-2026"});
        ingresar("admin", "admin123", true);
        Usuario sesion = SesionActual.obtenerUsuario();
        Usuario aDespues = udao.buscarPorNombreUsuario("admin");
        caso("SEG-18", "El diálogo no acepta claves inválidas y muestra el motivo", erroresVistos.size() == 2 && erroresVistos.get(0).contains("al menos 8") && erroresVistos.get(1).contains("distinta"), erroresVistos.toString());
        caso("SEG-19", "Con una clave válida entra, guarda la nueva y quita la marca", sesion != null && "admin".equals(sesion.getNombreUsuario())
                && !aDespues.isDebeCambiarClave() && SeguridadUtil.verificar("Elvis-Admin-2026", aDespues.getSal(), aDespues.getContrasenaHash()), null);
        boolean evento = columna("SELECT accion FROM log_auditoria WHERE accion = 'usuario_clave_cambiada'").size() == 1;
        caso("SEG-20", "El cambio queda en la bitácora", evento, null);

        abrirLogin(); SesionActual.cerrarSesion();
        String e3 = ingresar("admin", "admin123", false);
        caso("SEG-21", "admin123 ya no sirve después del cambio", SesionActual.obtenerUsuario() == null && e3.contains("incorrectos"), e3);
        abrirLogin(); SesionActual.cerrarSesion(); dialogosVistos = 0;
        ingresar("admin", "Elvis-Admin-2026", false);
        caso("SEG-22", "Con la clave nueva entra directo, sin diálogo", SesionActual.obtenerUsuario() != null && dialogosVistos == 0, null);

        abrirLogin(); SesionActual.cerrarSesion();
        ingresar("antigua", "vieja-123", false);
        Usuario antDespues = udao.buscarPorNombreUsuario("antigua");
        caso("SEG-23", "Login con hash antiguo: entra y el hash pasa a PBKDF2", SesionActual.obtenerUsuario() != null
                && antDespues.getContrasenaHash().startsWith("pbkdf2$") && SeguridadUtil.verificar("vieja-123", antDespues.getSal(), antDespues.getContrasenaHash()), antDespues.getContrasenaHash().substring(0, 10));

        udao.restablecerContrasena(antDespues.getId(), "Temporal-99");
        caso("SEG-24", "Restablecer clave (administrador) vuelve a exigir el cambio", udao.buscarPorNombreUsuario("antigua").isDebeCambiarClave(), null);

        PruebasElvis.fx(() -> { escenario.close(); return null; });
    }
}
