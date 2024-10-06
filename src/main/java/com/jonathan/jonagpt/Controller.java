package com.jonathan.jonagpt;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Controller {
    @FXML
    public Button cancel;
    @FXML
    public Button clear;
    @FXML
    public Button media;
    @FXML
    public Button submit;
    @FXML
    public Button submit0;
    @FXML
    public TextField texto;
    @FXML
    public VBox box;
    @FXML
    public ScrollPane scroll;
    private boolean isFirst = false;
    private CompletableFuture<HttpResponse<InputStream>> streamRequest;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private InputStream currentInputStream;
    private Future<?> streamReadingTask;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();




    @FXML
    public void initialize() {
        scroll.setFitToWidth(true);
    }


    @FXML
    public void enviarMensaje() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/Layout_message_you.fxml")));
            Node layout2 = loader.load();

            ControllerMesYou controllerMesYou = loader.getController();
            controllerMesYou.establecerTexto(texto.getText());

            box.getChildren().add(layout2);

            scroll.layout();
            scroll.setVvalue(1.0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        texto.clear();
    }

    @FXML
    private void callStream(ActionEvent event) {
        texto.setText(""); // Clear the textInfo

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"model\": \"llama3.2:1b\", \"prompt\": \"Why is the sky blue?\", \"stream\": true}"))
                .build();

        Platform.runLater(() -> texto.setText("Wait stream ..."));

        isFirst = true;
        streamRequest = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    currentInputStream = response.body();
                    streamReadingTask = executorService.submit(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentInputStream))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                JSONObject jsonResponse = new JSONObject(line);
                                String responseText = jsonResponse.getString("response");
                                if (isFirst) {
                                    Platform.runLater(() -> texto.setText(responseText));
                                    isFirst = false;
                                } else {
                                    Platform.runLater(() -> texto.setText(texto.getText() + responseText));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Platform.runLater(() -> {
                                texto.setText("Error during streaming.");
                            });
                        } finally {
                            try {
                                if (currentInputStream != null) {
                                    System.out.println("Cancelling InputStream in finally");
                                    currentInputStream.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    return response;
                });

    }


}