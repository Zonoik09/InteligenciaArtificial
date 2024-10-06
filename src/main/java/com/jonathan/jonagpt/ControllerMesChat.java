package com.jonathan.jonagpt;

import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ControllerMesChat {

    @FXML
    public TextFlow Area = new TextFlow(); // TextFlow que se actualiza con el nuevo texto

    public void addText(String text) {
        if (Area == null) {
            System.out.println("El TextFlow no está inicializado.");
            return;
        }

        // Crear un nuevo nodo de texto y añadirlo al TextFlow existente
        Text message = new Text(text);
        Area.getChildren().add(message);
    }

}
