package com.example.trivzclient.screen;

import com.example.trivzclient.ApiClient;
import com.example.trivzclient.ClientSession;
import com.example.trivzclient.LiveClient;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameScreen {

    private final Stage stage;
    private final Long roomId;
    private final LiveClient live = new LiveClient();

    private final Label title = new Label();
    private final Label hostLabel = new Label();
    private final Label questionLabel = new Label("Waiting for the host to start...");
    private final Label timerLabel = new Label("--");
    private final Label progressLabel = new Label("");
    private final Label roundLabel = new Label("");
    private final Label statusLabel = new Label("");
    private final Label revealLabel = new Label("");
    private final TextField answerField = new TextField();
    private final Button startBtn = new Button("Start Game");
    private final Button submitBtn = new Button("Submit");

    private final VBox playArea = new VBox(10);
    private final VBox playersBox = new VBox(4);

    private final Map<String, Long> pointsByUsername = new LinkedHashMap<>();

    private volatile Long currentQuestionId;
    private volatile boolean isHost;
    private volatile String currentRoomStatus = "LOBBY";
    private volatile LocalDateTime roundDeadline;
    private volatile boolean navigatedAway;

    private Timeline roundClock;
    private Timeline revealCountdown;

    private static final String GREEN_BG = "-fx-background-color: #2e8b57; -fx-text-fill: white;";
    private static final String RED_BG   = "-fx-background-color: #b22222; -fx-text-fill: white;";

    public GameScreen(Stage stage, Long roomId) {
        this.stage = stage;
        this.roomId = roomId;
    }

    public void show() {
        title.setText("Room " + roomId);
        title.getStyleClass().add("title");
        hostLabel.getStyleClass().add("subtitle");
        questionLabel.getStyleClass().add("question");
        timerLabel.getStyleClass().add("timer");
        progressLabel.getStyleClass().add("progress");
        roundLabel.getStyleClass().add("subtitle");
        statusLabel.getStyleClass().add("status");

        revealLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #5fff8a; -fx-font-weight: bold;");
        revealLabel.setVisible(false);
        revealLabel.setManaged(false);

        startBtn.getStyleClass().add("primary");
        startBtn.setOnAction(e -> startGame());
        startBtn.setVisible(false);
        startBtn.setManaged(false);

        submitBtn.getStyleClass().add("primary");
        submitBtn.setOnAction(e -> submitAnswer());

        Button leaveBtn = new Button("Leave");
        leaveBtn.getStyleClass().add("secondary");
        leaveBtn.setOnAction(e -> leave());

        answerField.setPromptText("type your answer");

        HBox answerRow = new HBox(8, answerField, submitBtn);
        HBox.setHgrow(answerField, Priority.ALWAYS);
        answerRow.setAlignment(Pos.CENTER_LEFT);

        HBox topRow = new HBox(10, startBtn, leaveBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);

        playArea.getChildren().addAll(progressLabel, roundLabel, questionLabel, timerLabel, answerRow, revealLabel);
        playArea.getStyleClass().add("card");

        Label playersTitle = new Label("Players (this round)");
        playersTitle.getStyleClass().add("subtitle");
        VBox playersCard = new VBox(8, playersTitle, playersBox);
        playersCard.getStyleClass().add("card");
        playersCard.setMinWidth(240);

        HBox mainRow = new HBox(15, playArea, playersCard);
        HBox.setHgrow(playArea, Priority.ALWAYS);

        VBox root = new VBox(15, title, hostLabel, topRow, mainRow, statusLabel);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 920, 620);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setTitle("Trivz — Room " + roomId);
        stage.setOnCloseRequest(e -> {
            stopRoundClock();
            stopRevealCountdown();
            live.disconnect();
        });
        stage.show();

        startRoundClock();

        loadRoomInfo();
        loadMembers();
        loadLeaderboard();
        connectLive();
        refreshCurrent();
    }

    private void startRoundClock() {
        roundClock = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateRoundLabel()));
        roundClock.setCycleCount(Timeline.INDEFINITE);
        roundClock.play();
    }

    private void stopRoundClock() {
        if (roundClock != null) roundClock.stop();
    }

    private void stopRevealCountdown() {
        if (revealCountdown != null) {
            revealCountdown.stop();
            revealCountdown = null;
        }
    }

    private void updateRoundLabel() {
        if (roundDeadline == null || !"IN_PROGRESS".equalsIgnoreCase(currentRoomStatus)) {
            roundLabel.setText("");
            return;
        }
        long secs = java.time.Duration.between(LocalDateTime.now(), roundDeadline).getSeconds();
        if (secs < 0) secs = 0;
        long m = secs / 60;
        long s = secs % 60;
        roundLabel.setText("Round time left: " + m + "m " + String.format("%02d", s) + "s");
    }

    private void loadRoomInfo() {
        Task<Map> task = new Task<>() {
            @Override
            protected Map call() throws Exception {
                return ApiClient.get("/api/rooms/" + roomId, Map.class);
            }
        };
        task.setOnSucceeded(e -> {
            Map m = task.getValue();
            Object hostId = m.get("hostPlayerId");
            Object hostName = m.get("hostUsername");
            Object roomStatus = m.get("status");
            Object roomName = m.get("name");
            if (roomName != null) {
                title.setText(String.valueOf(roomName));
                stage.setTitle("Trivz — " + roomName);
            }
            Long hostPlayerId = (hostId instanceof Number) ? ((Number) hostId).longValue() : null;
            isHost = hostPlayerId != null && hostPlayerId.equals(ClientSession.playerId);
            String label;
            if (isHost) label = "You are the host";
            else if (hostName != null) label = "Host: " + hostName;
            else label = "Host: player " + hostPlayerId;
            hostLabel.setText(label);

            applyStatus(String.valueOf(roomStatus));
        });
        Thread th = new Thread(task, "trivz-room-info");
        th.setDaemon(true);
        th.start();
    }

    private void loadMembers() {
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override
            protected List<Map<String, Object>> call() throws Exception {
                return ApiClient.get("/api/rooms/" + roomId + "/members",
                        new TypeReference<List<Map<String, Object>>>() {});
            }
        };
        task.setOnSucceeded(e -> {
            for (Map<String, Object> m : task.getValue()) {
                String name = String.valueOf(m.get("playerUsername"));
                pointsByUsername.putIfAbsent(name, 0L);
            }
            renderPlayers();
        });
        Thread th = new Thread(task, "trivz-members");
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
        task.setOnSucceeded(e -> {
            applyLeaderboard(task.getValue());
            renderPlayers();
        });
        Thread th = new Thread(task, "trivz-board-init");
        th.setDaemon(true);
        th.start();
    }

    private void connectLive() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                live.connect();
                live.subscribe("/topic/room/" + roomId + "/question",    Map.class,  m -> onQuestion(m));
                live.subscribe("/topic/room/" + roomId + "/tick",        Map.class,  m -> onTick(m));
                live.subscribe("/topic/room/" + roomId + "/score",       Map.class,  m -> onScore(m));
                live.subscribe("/topic/room/" + roomId + "/status",      Map.class,  m -> onStatus(m));
                live.subscribe("/topic/room/" + roomId + "/leaderboard", List.class, l -> onLeaderboard(l));
                live.subscribe("/topic/room/" + roomId + "/members",     Map.class,  m -> onMember(m));
                live.subscribe("/topic/room/" + roomId + "/reveal",      Map.class,  m -> onReveal(m));
                return null;
            }
        };
        task.setOnFailed(e -> statusLabel.setText("WebSocket: " + task.getException().getMessage()));
        Thread th = new Thread(task, "trivz-ws");
        th.setDaemon(true);
        th.start();
    }

    @SuppressWarnings("rawtypes")
    private void onQuestion(Object payload) {
        Map m = (Map) payload;
        currentQuestionId = ((Number) m.get("questionId")).longValue();
        Object idx = m.get("index");
        Object total = m.get("total");
        progressLabel.setText("Question " + (((Number) idx).intValue() + 1) + " / " + total);
        questionLabel.setText(String.valueOf(m.get("prompt")));
        Object deadline = m.get("roundDeadline");
        if (deadline != null) {
            try { roundDeadline = LocalDateTime.parse(String.valueOf(deadline)); }
            catch (Exception ignored) {}
        }
        answerField.clear();
        answerField.setStyle("");
        revealLabel.setVisible(false);
        revealLabel.setManaged(false);
        stopRevealCountdown();
        statusLabel.setText("");
        statusLabel.setStyle("");
        applyStatus("IN_PROGRESS");
    }

    @SuppressWarnings("rawtypes")
    private void onReveal(Object payload) {
        Map m = (Map) payload;
        String answer = String.valueOf(m.get("answer"));
        int seconds = ((Number) m.get("seconds")).intValue();
        revealLabel.setText("Correct answer: " + answer);
        revealLabel.setVisible(true);
        revealLabel.setManaged(true);
        submitBtn.setDisable(true);
        answerField.setDisable(true);

        // Real ticking countdown via Timeline
        stopRevealCountdown();
        final int[] remaining = { seconds };
        timerLabel.setText("Next question in " + remaining[0] + "s");
        revealCountdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remaining[0]--;
            if (remaining[0] >= 0) {
                timerLabel.setText("Next question in " + remaining[0] + "s");
            }
            if (remaining[0] <= 0) stopRevealCountdown();
        }));
        revealCountdown.setCycleCount(seconds);
        revealCountdown.play();
    }

    @SuppressWarnings("rawtypes")
    private void onTick(Object payload) {
        Map m = (Map) payload;
        timerLabel.setText("Time left: " + m.get("remaining") + "s");
    }

    @SuppressWarnings("rawtypes")
    private void onScore(Object payload) {
        Map m = (Map) payload;
        boolean correct = Boolean.TRUE.equals(m.get("correct"));
        if (!correct) return;
        // Don't overwrite our own "Correct! +10 pts" message — submitAnswer's response
        // handler already updated the status label for the submitter.
        Object pid = m.get("playerId");
        if (pid instanceof Number n && ClientSession.playerId != null
                && n.longValue() == ClientSession.playerId) {
            return;
        }
        Object username = m.get("playerUsername");
        Object pts = m.get("points");
        statusLabel.setText((username == null ? "Someone" : username) + " got it right! +" + pts + " pts");
        statusLabel.setStyle("-fx-text-fill: #5fff8a; -fx-font-weight: bold;");
    }

    @SuppressWarnings("rawtypes")
    private void onStatus(Object payload) {
        Map m = (Map) payload;
        Object s = m.get("status");
        applyStatus(String.valueOf(s));
    }

    private void applyStatus(String roomStatus) {
        currentRoomStatus = roomStatus == null ? "LOBBY" : roomStatus.toUpperCase();
        boolean inProgress = "IN_PROGRESS".equals(currentRoomStatus);
        boolean finished = "FINISHED".equals(currentRoomStatus);
        boolean lobby = "LOBBY".equals(currentRoomStatus);

        startBtn.setVisible(isHost && lobby);
        startBtn.setManaged(isHost && lobby);

        submitBtn.setDisable(!inProgress);
        answerField.setDisable(!inProgress);

        if (finished && !navigatedAway) {
            navigatedAway = true;
            stopRoundClock();
            stopRevealCountdown();
            live.disconnect();
            Platform.runLater(() -> new PodiumScreen(stage, roomId, true).show());
        }
    }

    @SuppressWarnings("rawtypes")
    private void onLeaderboard(Object payload) {
        List list = (List) payload;
        applyLeaderboard(list);
        renderPlayers();
    }

    @SuppressWarnings("rawtypes")
    private void applyLeaderboard(List rows) {
        if (rows == null) return;
        for (String username : pointsByUsername.keySet()) {
            pointsByUsername.put(username, 0L);
        }
        for (Object row : rows) {
            Map r = (Map) row;
            String name = String.valueOf(r.get("username"));
            Object pts = r.get("totalPoints");
            long val = (pts instanceof Number) ? ((Number) pts).longValue() : 0L;
            pointsByUsername.put(name, val);
        }
    }

    @SuppressWarnings("rawtypes")
    private void onMember(Object payload) {
        Map m = (Map) payload;
        String event = String.valueOf(m.get("event"));
        Map member = (Map) m.get("member");
        if (member == null) return;
        String username = String.valueOf(member.get("playerUsername"));
        if ("JOIN".equals(event)) {
            pointsByUsername.putIfAbsent(username, 0L);
            // A returning player keeps the points they already had — refresh from the
            // server so their Score rows (preserved across leave/rejoin) reflect here.
            loadLeaderboard();
        } else if ("LEAVE".equals(event)) {
            pointsByUsername.remove(username);
        }
        renderPlayers();
    }

    private void renderPlayers() {
        playersBox.getChildren().clear();
        if (pointsByUsername.isEmpty()) {
            Label empty = new Label("No players yet");
            empty.getStyleClass().add("subtitle");
            playersBox.getChildren().add(empty);
            return;
        }
        pointsByUsername.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> {
                    String youSuffix = e.getKey().equals(ClientSession.username) ? "  (you)" : "";
                    Label l = new Label(e.getKey() + youSuffix + " — " + e.getValue() + " pts");
                    l.getStyleClass().add("leaderboard");
                    playersBox.getChildren().add(l);
                });
    }

    private void startGame() {
        runAsync(() -> {
            Map<String, Object> body = new HashMap<>();
            body.put("questionCount", 10);
            return ApiClient.post("/api/rooms/" + roomId + "/game/start", body, Map.class, true);
        });
    }

    private void submitAnswer() {
        if (currentQuestionId == null) {
            statusLabel.setText("No active question");
            return;
        }
        String text = answerField.getText();
        if (text == null || text.isBlank()) {
            statusLabel.setText("Type an answer");
            return;
        }
        runAsyncWithResult(
                () -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("questionId", currentQuestionId);
                    body.put("answerText", text);
                    body.put("timeTakenSeconds", 0);
                    return ApiClient.post("/api/rooms/" + roomId + "/scores/submit", body, Map.class, true);
                },
                resp -> {
                    boolean correct = Boolean.TRUE.equals(resp.get("correct"));
                    answerField.setStyle(correct ? GREEN_BG : RED_BG);
                    submitBtn.setDisable(true);
                    statusLabel.setText(correct ? "Correct! +" + resp.get("points") + " pts" : "Wrong");
                });
    }

    private void leave() {
        runAsync(() -> {
            ApiClient.post("/api/rooms/" + roomId + "/leave", Map.of(), Map.class, true);
            return null;
        });
        stopRoundClock();
        stopRevealCountdown();
        live.disconnect();
        new LobbyScreen(stage).show();
    }

    private <T> void runAsync(SafeCallable<T> work) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            statusLabel.setText(t == null ? "error" : t.getMessage());
        });
        Thread th = new Thread(task, "trivz-game");
        th.setDaemon(true);
        th.start();
    }

    @SuppressWarnings("rawtypes")
    private void runAsyncWithResult(SafeCallable<Map> work, java.util.function.Consumer<Map> onOk) {
        Task<Map> task = new Task<>() {
            @Override protected Map call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            Map r = task.getValue();
            if (r != null) onOk.accept(r);
        }));
        task.setOnFailed(e -> {
            Throwable t = task.getException();
            statusLabel.setText(t == null ? "error" : t.getMessage());
        });
        Thread th = new Thread(task, "trivz-game");
        th.setDaemon(true);
        th.start();
    }

    private void refreshCurrent() {
        Task<Map> task = new Task<>() {
            @Override protected Map call() {
                try {
                    return ApiClient.get("/api/rooms/" + roomId + "/game/current", Map.class);
                } catch (Exception ignored) {
                    return null;
                }
            }
        };
        task.setOnSucceeded(e -> {
            Map m = task.getValue();
            if (m != null) onQuestion(m);
        });
        Thread th = new Thread(task, "trivz-game-refresh");
        th.setDaemon(true);
        th.start();
    }

    @FunctionalInterface
    interface SafeCallable<T> { T call() throws Exception; }
}
