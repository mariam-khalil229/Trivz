package com.example.trivzclient.screen;

import com.example.trivzclient.ApiClient;
import com.example.trivzclient.ClientSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Map;

public class LoginScreen {

    private final Stage stage;

    public LoginScreen(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        Label title = new Label("Trivz");
        title.getStyleClass().add("title");

        Label subtitle = new Label("multiplayer trivia");
        subtitle.getStyleClass().add("subtitle");

        TextField serverField = new TextField(ClientSession.baseUrl);
        serverField.setPromptText("server URL e.g. http://192.168.1.10:8080");

        TextField usernameField = new TextField();
        usernameField.setPromptText("username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("password");

        Label status = new Label();
        status.getStyleClass().add("status");

        Button loginBtn = new Button("Login");
        Button registerBtn = new Button("Register");
        loginBtn.getStyleClass().add("primary");
        registerBtn.getStyleClass().add("secondary");

        loginBtn.setOnAction(e -> {
            ClientSession.setServer(serverField.getText());
            doLogin(usernameField.getText(), passwordField.getText(), status);
        });
        registerBtn.setOnAction(e -> {
            ClientSession.setServer(serverField.getText());
            doRegister(usernameField.getText(), passwordField.getText(), status);
        });

        VBox card = new VBox(10,
                labeled("Server", serverField),
                labeled("Username", usernameField),
                labeled("Password", passwordField));
        card.getStyleClass().add("card");

        HBox buttons = new HBox(10, loginBtn, registerBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(15, title, subtitle, card, buttons, status);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 460, 520);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setTitle("Trivz — Login");
        stage.show();
    }

    private VBox labeled(String label, javafx.scene.Node field) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        VBox box = new VBox(4, l, field);
        return box;
    }

    private void doLogin(String username, String password, Label status) {
        runAsync(() -> {
            login(username, password);
            return null;
        }, status, () -> new LobbyScreen(stage).show());
    }

    private void doRegister(String username, String password, Label status) {
        runAsync(() -> {
            Map<String, String> body = Map.of(
                    "username", username,
                    "password", password
            );
            ApiClient.post("/api/players", body, Map.class, false);
            login(username, password);
            return null;
        }, status, () -> new LobbyScreen(stage).show());
    }

    private void login(String username, String password) throws Exception {
        Map<String, String> body = Map.of("username", username, "password", password);
        Map<?, ?> resp = ApiClient.post("/api/auth/login", body, Map.class, false);
        ClientSession.token = (String) resp.get("token");
        ClientSession.username = username;

        Map<?, ?> me = ApiClient.get("/api/auth/me", Map.class);
        Object id = me.get("id");
        ClientSession.playerId = (id instanceof Number) ? ((Number) id).longValue() : null;
        Object r = me.get("role");
        ClientSession.role = r == null ? "USER" : String.valueOf(r);
    }

    private void runAsync(SafeCallable<?> work, Label status, Runnable onSuccess) {
        status.setText("");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                work.call();
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(onSuccess));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            status.setText(t == null ? "error" : t.getMessage());
        });
        Thread th = new Thread(task, "trivz-login");
        th.setDaemon(true);
        th.start();
    }

    @FunctionalInterface
    interface SafeCallable<T> {
        T call() throws Exception;
    }
}
