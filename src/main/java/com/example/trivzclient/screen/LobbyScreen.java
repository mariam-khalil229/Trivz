package com.example.trivzclient.screen;

import com.example.trivzclient.ApiClient;
import com.example.trivzclient.Async;
import com.example.trivzclient.ClientSession;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;

public class LobbyScreen {

    private final Stage stage;
    private final ObservableList<Map<String, Object>> rooms = FXCollections.observableArrayList();
    private final Label status = new Label();

    public LobbyScreen(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        Label title = new Label("Lobby");
        title.getStyleClass().add("title");

        Label whoami = new Label("Logged in as " + ClientSession.username);
        whoami.getStyleClass().add("subtitle");

        TableView<Map<String, Object>> table = buildTable();
        table.setItems(rooms);
        VBox.setVgrow(table, Priority.ALWAYS);

        TextField roomNameField = new TextField();
        roomNameField.setPromptText("room name");

        TextField maxPlayersField = new TextField();
        maxPlayersField.setPromptText("max (default 8)");
        maxPlayersField.setMaxWidth(140);

        ChoiceBox<String> difficultyBox = new ChoiceBox<>(
                FXCollections.observableArrayList("any", "easy", "medium", "hard"));
        difficultyBox.getSelectionModel().select(0);

        Button createBtn = new Button("Create Room");
        createBtn.getStyleClass().add("primary");

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("secondary");

        Button openBtn = new Button("Open Selected");
        openBtn.getStyleClass().add("primary");

        Button adminBtn = new Button("Admin");
        adminBtn.getStyleClass().add("secondary");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("secondary");

        refreshBtn.setOnAction(e -> refreshRooms());
        createBtn.setOnAction(e -> createRoom(
                roomNameField.getText(),
                maxPlayersField.getText(),
                difficultyBox.getValue()));
        openBtn.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                status.setText("Select a room first");
                return;
            }
            openRoom(sel);
        });
        adminBtn.setOnAction(e -> new AdminScreen(stage).show());
        logoutBtn.setOnAction(e -> {
            ClientSession.token = null;
            ClientSession.username = null;
            ClientSession.playerId = null;
            ClientSession.role = null;
            new LoginScreen(stage).show();
        });

        HBox createRow = new HBox(8,
                roomNameField, maxPlayersField,
                new Label("Difficulty:"), difficultyBox,
                createBtn);
        HBox.setHgrow(roomNameField, Priority.ALWAYS);
        createRow.setAlignment(Pos.CENTER_LEFT);

        VBox createCard = new VBox(8, new Label("Create a room"), createRow);
        createCard.getStyleClass().add("card");

        boolean isAdmin = "ADMIN".equalsIgnoreCase(ClientSession.role);
        adminBtn.setVisible(isAdmin);
        adminBtn.setManaged(isAdmin);

        HBox actionRow = new HBox(8, refreshBtn, openBtn, adminBtn, logoutBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(15, title, whoami, table, createCard, actionRow, status);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 820, 580);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setTitle("Trivz — Lobby");
        stage.show();

        refreshRooms();
    }

    private TableView<Map<String, Object>> buildTable() {
        TableView<Map<String, Object>> table = new TableView<>();

        TableColumn<Map<String, Object>, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().get("code"))));

        TableColumn<Map<String, Object>, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().get("name"))));

        TableColumn<Map<String, Object>, String> diffCol = new TableColumn<>("Difficulty");
        diffCol.setCellValueFactory(c -> {
            Object d = c.getValue().get("difficulty");
            return new SimpleStringProperty(d == null ? "any" : String.valueOf(d));
        });

        TableColumn<Map<String, Object>, String> playersCol = new TableColumn<>("Players");
        playersCol.setCellValueFactory(c -> {
            Object cur = c.getValue().get("memberCount");
            Object max = c.getValue().get("maxPlayers");
            String label = (cur == null ? "?" : cur) + " / " + (max == null ? "?" : max);
            return new SimpleStringProperty(label);
        });

        TableColumn<Map<String, Object>, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(friendlyStatus(c.getValue().get("status"))));

        TableColumn<Map<String, Object>, String> hostCol = new TableColumn<>("Host");
        hostCol.setCellValueFactory(c -> {
            Object hostId = c.getValue().get("hostPlayerId");
            Object hostName = c.getValue().get("hostUsername");
            String label;
            if (hostId instanceof Number n && ClientSession.playerId != null && n.longValue() == ClientSession.playerId) {
                label = "you";
            } else if (hostName != null) {
                label = String.valueOf(hostName);
            } else {
                label = String.valueOf(hostId);
            }
            return new SimpleStringProperty(label);
        });

        table.getColumns().add(codeCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(diffCol);
        table.getColumns().add(playersCol);
        table.getColumns().add(statusCol);
        table.getColumns().add(hostCol);

        codeCol.setPrefWidth(70);
        nameCol.setPrefWidth(220);
        diffCol.setPrefWidth(90);
        playersCol.setPrefWidth(80);
        statusCol.setPrefWidth(150);
        hostCol.setPrefWidth(120);

        return table;
    }

    private String friendlyStatus(Object raw) {
        String s = String.valueOf(raw);
        return switch (s.toUpperCase()) {
            case "LOBBY" -> "Waiting for host";
            case "IN_PROGRESS" -> "In progress";
            case "FINISHED" -> "Expired";
            default -> s;
        };
    }

    private void refreshRooms() {
        runAsync(() -> ApiClient.get("/api/rooms", new TypeReference<List<Map<String, Object>>>() {}),
                list -> {
                    rooms.setAll(list);
                    status.setText("Loaded " + list.size() + " rooms");
                });
    }

    private void createRoom(String name, String maxText, String difficulty) {
        if (name == null || name.isBlank()) {
            status.setText("Room name required");
            return;
        }
        Integer max = null;
        try { if (maxText != null && !maxText.isBlank()) max = Integer.parseInt(maxText.trim()); }
        catch (NumberFormatException ex) { status.setText("max players must be a number"); return; }

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", name);
        if (max != null) body.put("maxPlayers", max);
        if (difficulty != null && !"any".equalsIgnoreCase(difficulty)) {
            body.put("difficulty", difficulty);
        }

        runAsync(() -> ApiClient.post("/api/rooms", body, Map.class, true),
                created -> {
                    status.setText("Created room " + created.get("code"));
                    Long id = ((Number) created.get("id")).longValue();
                    ClientSession.currentRoomId = id;
                    new GameScreen(stage, id).show();
                });
    }

    private void openRoom(Map<String, Object> roomRow) {
        String s = String.valueOf(roomRow.get("status"));
        Long id = ((Number) roomRow.get("id")).longValue();
        if ("FINISHED".equalsIgnoreCase(s)) {
            // Read-only podium of the most recent finished round
            new PodiumScreen(stage, id, false).show();
            return;
        }
        // LOBBY or IN_PROGRESS — server will reject if room is at capacity
        runAsync(() -> {
            ApiClient.post("/api/rooms/" + id + "/join", Map.of(), Map.class, true);
            return id;
        }, opened -> {
            ClientSession.currentRoomId = opened;
            new GameScreen(stage, opened).show();
        });
    }

    private <T> void runAsync(Async.SafeCallable<T> work, java.util.function.Consumer<T> onOk) {
        status.setText("...");
        Async.run("trivz-lobby", work, onOk, status::setText);
    }
}
