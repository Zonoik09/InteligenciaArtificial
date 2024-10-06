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
    private CompletableFuture<HttpResponse<InputStream>> streamRequest;
    private CompletableFuture<HttpResponse<String>> completeRequest;
    private AtomicBoolean isCancelled = new AtomicBoolean(false);
    private InputStream currentInputStream;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> streamReadingTask;
    private boolean isFirst = true;
    private Map<Node, ControllerMesChat> layoutControllerMap = new HashMap<>();




    @FXML
    public void initialize() {
        scroll.setFitToWidth(true);
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
            sendMessageToOllama(texto.getText());
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
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

//                // Set the image in the ImageView
//                Image image = new Image(selectedFile.toURI().toString());
//                img.setImage(image);
//
//                // Set the Base64 string in the TextArea (was previously a Text element)
//                txt.setText(base64Image);

                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Limpia la pantalla
    public void clearScreen(ActionEvent actionEvent) {
        box.getChildren().clear();
    }

    @FXML
    public void sendMessageToOllama(String userMessage) throws IOException, InterruptedException {
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
                    StringBuilder completeResponse = new StringBuilder(); 

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            JSONObject jsonResponse = new JSONObject(line);
                            String responseText = jsonResponse.getString("response");

                            completeResponse.append(responseText);

                            Platform.runLater(() -> {
                                try {
                                    if (isFirst) {
                                        // La primera vez, creamos y cargamos el layout
                                        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/Layout_message_chat.fxml")));
                                        Node layout3 = loader.load();
                                        ControllerMesChat controllerMesChat = loader.getController();
                                        box.getChildren().add(layout3);
                                        layoutControllerMap.put(layout3, controllerMesChat);
                                        controllerMesChat.addText(completeResponse.toString());
                                        isFirst = false;
                                    } else {
                                        // Obtenemos el último layout añadido
                                        Node lastLayout = box.getChildren().get(box.getChildren().size() - 1);

                                        // Buscamos el controlador en el mapa
                                        ControllerMesChat controllerMesChat = layoutControllerMap.get(lastLayout);

                                        if (controllerMesChat != null) {
                                            // Añadimos el texto acumulado al layout existente
                                            controllerMesChat.addText(completeResponse.toString());
                                        }
                                    }

                                    // Asegurarnos de que el scroll baje al último mensaje
                                    scroll.layout();
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