package co.edu.usc.elvis.util;

import javafx.scene.control.Label;

/**
 * Los avisos de exito/error de cada pantalla solo se ven (y ocupan espacio)
 * cuando tienen texto; al borrar el texto desaparecen solos.
 */
public class Mensajes {

    private Mensajes() {
    }

    public static void configurar(Label... etiquetas) {
        for (Label e : etiquetas) {
            e.visibleProperty().bind(e.textProperty().isNotEmpty());
            e.managedProperty().bind(e.textProperty().isNotEmpty());
        }
    }
}