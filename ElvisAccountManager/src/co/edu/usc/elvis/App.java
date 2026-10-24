package co.edu.usc.elvis;

import co.edu.usc.elvis.dao.ConexionBD;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stagePrincipal) throws Exception {
        ConexionBD.inicializarBaseDeDatos();

        Parent raiz = FXMLLoader.load(getClass().getResource("/co/edu/usc/elvis/view/Login.fxml"));
        Scene escena = new Scene(raiz, 1200, 750);
        escena.getStylesheets().add(
                getClass().getResource("/co/edu/usc/elvis/view/estilos.css").toExternalForm());

        stagePrincipal.setTitle("Servidor ELVIS - USC");
        stagePrincipal.setScene(escena);
        stagePrincipal.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}