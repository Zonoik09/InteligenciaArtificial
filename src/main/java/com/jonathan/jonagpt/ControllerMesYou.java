package com.jonathan.jonagpt;

import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class ControllerMesYou {
    @FXML
    public TextFlow Area = new TextFlow();

    public String establecerTexto(String text) {
        Text message = new Text(text);
        message.wrappingWidthProperty().bind(Area.widthProperty());
        Area.getChildren().add(message);
        return text;
    }
}
