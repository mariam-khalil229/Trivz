package com.example.trivzclient.screen;

import com.example.trivzclient.ApiClient;
import com.example.trivzclient.ClientSession;
import com.example.trivzclient.LiveClient;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PodiumScreen {

    private final Stage stage;
    private final Long roomId;
    private final boolean live;

    private final LiveClient liveClient = new LiveClient();
    private final Button playAgainBtn = new Button("Play Again");
    private final Button backBtn = new Button("Back to Lobby");
    private final Label statusLabel = new Label();
    private final VBox board = new VBox(10);

    private volatile boolean isHost;
    private volatile boolean navigatedAway;

    /**
     * @param live true when arriving from a freshly-finished game (so the screen subscribes
     *             to /status and auto-navigates back to GameScreen if the host plays again).
     *             false for "view expired room" from the lobby — read-only podium.
     */
    public PodiumScreen(Stage stage, Long roomId, boolean live) {
        this.stage = stage;
        this.roomId = roomId;
        this.live = live;
    }

    public void show() {
        Label title = new Label("Final Standings");
        title.getStyleClass().add("title");

        Label subtitle = new Label(live ? "Round complete" : "Most recent round (read-only)");
        subtitle.getStyleClass().add("subtitle");

        board.setAlignment(Pos.CENTER);
        board.getStyleClass().add("card");

        playAgainBtn.getStyleClass().add("primary");
        playAgainBtn.setOnAction(e -> playAgain());
        playAgainBtn.setVisible(false);
        playAgainBtn.setManaged(false);

        backBtn.getStyleClass().add("secondary");
        backBtn.setOnAction(e -> {
            stopAndDisconnect();
            new LobbyScreen(stage).show();
        });

        statusLabel.getStyleClass().add("status");

        HBox actions = new HBox(10, playAgainBtn, backBtn);
        actions.setAlignment(Pos.CENTER);

        VBox root = new VBox(20, title, subtitle, board, actions, statusLabel);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 560, 600);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setTitle("Trivz — Podium");
        stage.setOnCloseRequest(e -> stopAndDisconnect());
        stage.show();

        loadHostInfo();
        loadLeaderboard();
        if (live) connectLive();
    }

    private void stopAndDisconnect() {
        liveClient.disconnect();
    }

    @SuppressWarnings("rawtypes")
    private void loadHostInfo() {
        Task<Map> task = new Task<>() {
            @Override
            protected Map call() throws Exception {
                return ApiClient.get("/api/rooms/" + roomId, Map.class);
            }
        };
        task.setOnSucceeded(e -> {
            Map m = task.getValue();
            Object hostId = m.get("hostPlayerId");
            Long hostPlayerId = (hostId instanceof Number) ? ((Number) hostId).longValue() : null;
            isHost = hostPlayerId != null && hostPlayerId.equals(ClientSession.playerId);
            // Play Again only when the host just finished a round (live=true).
            // Expired rooms opened from the lobby are read-only history.
            boolean canReplay = isHost && live;
            playAgainBtn.setVisible(canReplay);
            playAgainBtn.setManaged(canReplay);
        });
        Thread th = new Thread(task, "trivz-podium-host");
        th.setDaemon(true);
        th.start();
    }

    private void loadLeaderboard() {
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                return ApiClient.get("/api/rooms/" + roomId + "/scores/leaderboard",
                        new TypeReference<List<Map<String, Object>>>() {});
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> renderPodium(task.getValue())));
        task.setOnFailed(e -> {
            Label err = new Label("Failed to load: " + task.getException().getMessage());
            err.getStyleClass().add("status");
            board.getChildren().add(err);
        });
        Thread th = new Thread(task, "trivz-podium");
        th.setDaemon(true);
        th.start();
    }

    private void connectLive() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                liveClient.connect();
                liveClient.subscribe("/topic/room/" + roomId + "/status", Map.class, m -> onStatus(m));
                return null;
            }
        };
        task.setOnFailed(e -> statusLabel.setText("WebSocket: " + task.getException().getMessage()));
        Thread th = new Thread(task, "trivz-podium-ws");
        th.setDaemon(true);
        th.start();
    }

    @SuppressWarnings("rawtypes")
    private void onStatus(Object payload) {
        Map m = (Map) payload;
        String s = String.valueOf(m.get("status"));
        if ("IN_PROGRESS".equalsIgnoreCase(s) && !navigatedAway) {
            navigatedAway = true;
            stopAndDisconnect();
            Platform.runLater(() -> new GameScreen(stage, roomId).show());
        }
    }

    private void playAgain() {
        statusLabel.setText("Starting next round...");
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                Map<String, Object> body = new HashMap<>();
                body.put("questionCount", 10);
                @SuppressWarnings("unchecked")
                Map<String, Object> r = ApiClient.post("/api/rooms/" + roomId + "/game/start",
                        body, Map.class, true);
                return r;
            }
        };
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            statusLabel.setText(t == null ? "Failed to start" : t.getMessage());
        });
        // The /status WebSocket event will navigate everyone (including the host) to GameScreen.
        Thread th = new Thread(task, "trivz-play-again");
        th.setDaemon(true);
        th.start();
    }

    private void renderPodium(List<Map<String, Object>> rows) {
        board.getChildren().clear();
        if (rows == null || rows.isEmpty()) {
            Label empty = new Label("No scores recorded yet.");
            empty.getStyleClass().add("subtitle");
            board.getChildren().add(empty);
            return;
        }

        SequentialTransition seq = new SequentialTransition();
        int rank = 1;
        for (Map<String, Object> row : rows) {
            Label l = new Label(rank + ".  " + row.get("username") + "  —  " + row.get("totalPoints") + " pts");
            if (rank == 1) l.setStyle("-fx-font-size: 24px; -fx-text-fill: #ffd166; -fx-font-weight: bold;");
            else if (rank == 2) l.setStyle("-fx-font-size: 20px; -fx-text-fill: #cfd6e4; -fx-font-weight: bold;");
            else if (rank == 3) l.setStyle("-fx-font-size: 18px; -fx-text-fill: #e08a5f; -fx-font-weight: bold;");
            else l.setStyle("-fx-font-size: 14px; -fx-text-fill: #9aa3b8;");

            l.setOpacity(0);
            l.setTranslateY(20);
            board.getChildren().add(l);

            FadeTransition fade = new FadeTransition(Duration.millis(450), l);
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(450), l);
            slide.setFromY(20);
            slide.setToY(0);

            seq.getChildren().add(new ParallelTransition(fade, slide));
            rank++;
        }
        seq.play();
    }
}
