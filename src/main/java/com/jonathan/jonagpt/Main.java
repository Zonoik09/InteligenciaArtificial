package com.jonathan.jonagpt;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(@NotNull Stage stage) throws IOException {

        Parent largeroot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/layout.fxml")));
        Scene scene = new Scene(largeroot);

        stage.setTitle("ZonoGPT");
        stage.setWidth(1020);
        stage.setHeight(620);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch(args);

    }

}