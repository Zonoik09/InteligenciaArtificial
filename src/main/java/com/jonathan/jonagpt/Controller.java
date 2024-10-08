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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final HttpClient httpClient = HttpClient.newHttpClient();
    @FXML
    public ImageView image;
    private CompletableFuture<HttpResponse<InputStream>> streamRequest;
    private CompletableFuture<HttpResponse<String>> completeRequest;
    private AtomicBoolean isCancelled = new AtomicBoolean(false);
    private InputStream currentInputStream;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> streamReadingTask;
    private boolean isFirst = true;
    private Map<Node, ControllerMesChat> layoutControllerMap = new HashMap<>();
    private String base64Image;


    @FXML
    public void initialize() {
        scroll.setFitToWidth(true);
        texto.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (!texto.getText().trim().isEmpty()) {
                    enviarMensaje();
                }
            }
        });
    }

    // Envia el mensaje y se muestra en la interfaz
    @FXML
    public void enviarMensaje() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/Layout_message_you.fxml")));
            Node layout2 = loader.load();

            ControllerMesYou controllerMesYou = loader.getController();
            controllerMesYou.establecerTexto(texto.getText());
            box.getChildren().add(layout2);
            if (base64Image != null) {
                sendMessageWithImage(texto.getText(), base64Image);
            } else {
                sendMessageToOllama(texto.getText());
            }
            scroll.layout();
            scroll.setVvalue(1.0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        texto.clear();
    }

    // Esto es para cargar la imagen
    @FXML
    private void actionLoad() {
        File initialDirectory = new File("./");
        FileChooser fileChooser = new FileChooser();
        if (initialDirectory.exists()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) media.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            image.setVisible(true);
            image.setImage(new Image(selectedFile.toURI().toString()));
            try {
                // Read the image file
                BufferedImage bufferedImage = ImageIO.read(selectedFile);

                // Create a ByteArrayOutputStream to store the image bytes
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // Write the image to the output stream in PNG format
                ImageIO.write(bufferedImage, "png", outputStream);

                // Get the byte array
                byte[] imageBytes = outputStream.toByteArray();

                // Encode to Base64
                base64Image = Base64.getEncoder().encodeToString(imageBytes);

                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessageWithImage(String userMessage, String base64Image) {
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = String.format("{\"model\": \"llava-phi3\", \"prompt\": \"%s\", \"images\": [\"%s\"]}", userMessage, base64Image);

        Platform.runLater(() -> texto.setText("Processing..."));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpResponse::body)
                .thenAccept(inputStream -> {
                    StringBuilder responseContent = new StringBuilder();

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            responseContent.append(line);
                        }

                        // Aquí separas los fragmentos de la respuesta
                        String[] responses = responseContent.toString().split("\\}\\{");
                        boolean isComplete = false;
                        StringBuilder finalResponse = new StringBuilder();

                        for (String response : responses) {
                            // Asegúrate de corregir el formato JSON si es necesario
                            String jsonString = response.trim();
                            if (!jsonString.startsWith("{")) {
                                jsonString = "{" + jsonString;
                            }
                            if (!jsonString.endsWith("}")) {
                                jsonString = jsonString + "}";
                            }

                            JSONObject jsonResponse = new JSONObject(jsonString);
                            finalResponse.append(jsonResponse.getString("response"));

                            if (jsonResponse.getBoolean("done")) {
                                isComplete = true;
                            }
                        }


                        if (isComplete) {
                            Platform.runLater(() -> {
                                try {
                                    FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/Layout_message_chat.fxml")));
                                    Node layout4 = loader.load();
                                    ControllerMesChat controllerMesChat = loader.getController();
                                    box.getChildren().add(layout4);
                                    layoutControllerMap.put(layout4, controllerMesChat);
                                    controllerMesChat.addText(finalResponse.toString());
                                    scroll.setVvalue(1.0);
                                    texto.setText("Completed");

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        } else {
                            Platform.runLater(() -> texto.setText("Response not complete yet."));
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        Platform.runLater(() -> texto.setText("Error during processing"));
                    }
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() -> texto.setText("Error during request: " + ex.getMessage()));
                    return null;
                });
    }

    public void clearScreen(ActionEvent actionEvent) {
        base64Image = null;
        image.setVisible(false);
        box.getChildren().clear();
    }

    @FXML
    public void sendMessageToOllama(String userMessage) throws IOException, InterruptedException {
        isFirst = true;
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = String.format("{\"model\": \"llama3.2:1b\", \"prompt\": \"%s\", \"stream\": true}", userMessage);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpResponse::body)
                .thenAccept(inputStream -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            JSONObject jsonResponse = new JSONObject(line);
                            String responseText = jsonResponse.getString("response");

                            Platform.runLater(() -> {
                                try {
                                    if (isFirst) {
                                        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/Layout_message_chat.fxml")));
                                        Node layout3 = loader.load();
                                        ControllerMesChat controllerMesChat = loader.getController();
                                        box.getChildren().add(layout3);
                                        layoutControllerMap.put(layout3, controllerMesChat);
                                        controllerMesChat.addText(responseText);
                                        isFirst = false;
                                    } else {
                                        Node lastLayout = box.getChildren().get(box.getChildren().size() - 1);
                                        ControllerMesChat controllerMesChat = layoutControllerMap.get(lastLayout);
                                        if (controllerMesChat != null) {
                                            controllerMesChat.addText(responseText);
                                        }
                                    }
                                    scroll.setVvalue(1.0);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }









}