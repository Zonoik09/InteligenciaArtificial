package com.jonathan.jonagpt;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ControllerMesChat {

    @FXML
    public TextFlow Area = new TextFlow();
    public BorderPane borderPane;
    public Label labelGPT;

    public void addText(String text) {
        if (Area == null) {
            System.out.println("El TextFlow no está iniciado.");
            return;
        }
        // Crear y añadirlo al TextFlow
        Text message = new Text(text);
        Area.getChildren().add(message);
        borderPane.setPrefHeight(Area.getHeight() + labelGPT.getHeight() + labelGPT.getHeight());
        System.out.println(borderPane.getHeight());

    }

}
