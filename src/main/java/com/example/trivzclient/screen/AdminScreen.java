package com.example.trivzclient.screen;

import com.example.trivzclient.ApiClient;
import com.example.trivzclient.Async;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminScreen {

    private final Stage stage;
    private final ObservableList<Map<String, Object>> questions = FXCollections.observableArrayList();
    private final Label status = new Label();

    {
        status.setWrapText(true);
        status.setMaxWidth(880);
    }

    public AdminScreen(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        Label title = new Label("Admin — Question Bank");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Add / edit / delete questions used by every room");
        subtitle.getStyleClass().add("subtitle");

        ChoiceBox<String> difficultyFilter = new ChoiceBox<>(
                FXCollections.observableArrayList("any", "easy", "medium", "hard"));
        difficultyFilter.getSelectionModel().select(0);

        TableView<Map<String, Object>> table = buildTable();
        table.setItems(questions);
        VBox.setVgrow(table, Priority.ALWAYS);

        TextField promptField = new TextField();
        promptField.setPromptText("question prompt");

        ChoiceBox<String> difficultyField = new ChoiceBox<>(
                FXCollections.observableArrayList("easy", "medium", "hard"));
        difficultyField.getSelectionModel().select(0);

        TextField answersField = new TextField();
        answersField.setPromptText("accepted answers (comma-separated)");

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("primary");
        Button updateBtn = new Button("Update Selected");
        updateBtn.getStyleClass().add("secondary");
        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("secondary");
        Button clearBtn = new Button("Clear / Deselect");
        clearBtn.getStyleClass().add("secondary");
        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("secondary");
        Button backBtn = new Button("Back to Lobby");
        backBtn.getStyleClass().add("secondary");

        // Add is only enabled when no row is selected (otherwise the form holds a selected
        // question's data and adding would create a duplicate). Update/Delete are the inverse.
        updateBtn.setDisable(true);
        deleteBtn.setDisable(true);

        difficultyFilter.setOnAction(e -> refresh(difficultyFilter.getValue()));
        refreshBtn.setOnAction(e -> refresh(difficultyFilter.getValue()));
        backBtn.setOnAction(e -> new LobbyScreen(stage).show());

        addBtn.setOnAction(e -> save(null, promptField.getText(), difficultyField.getValue(),
                answersField.getText(), difficultyFilter, table));

        updateBtn.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { status.setText("Select a question first"); return; }
            Long id = ((Number) sel.get("id")).longValue();
            save(id, promptField.getText(), difficultyField.getValue(),
                    answersField.getText(), difficultyFilter, table);
        });

        deleteBtn.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { status.setText("Select a question first"); return; }
            Long id = ((Number) sel.get("id")).longValue();
            deleteQuestion(id, difficultyFilter, table);
        });

        clearBtn.setOnAction(e -> {
            table.getSelectionModel().clearSelection();
            promptField.clear();
            answersField.clear();
            difficultyField.getSelectionModel().select(0);
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, was, now) -> {
            boolean selected = (now != null);
            addBtn.setDisable(selected);
            updateBtn.setDisable(!selected);
            deleteBtn.setDisable(!selected);
            if (now == null) return;
            promptField.setText(String.valueOf(now.get("prompt")));
            Object d = now.get("difficulty");
            if (d != null) difficultyField.setValue(String.valueOf(d).toLowerCase());
            Object a = now.get("acceptedAnswers");
            if (a instanceof List<?> list) {
                answersField.setText(String.join(", ", list.stream().map(String::valueOf).toList()));
            } else {
                answersField.setText("");
            }
        });

        HBox formRow1 = new HBox(8, promptField);
        HBox.setHgrow(promptField, Priority.ALWAYS);
        HBox formRow2 = new HBox(8, new Label("Difficulty:"), difficultyField);
        formRow2.setAlignment(Pos.CENTER_LEFT);
        HBox formRow3 = new HBox(8, answersField);
        HBox.setHgrow(answersField, Priority.ALWAYS);
        HBox formRow4 = new HBox(8, addBtn, updateBtn, deleteBtn, clearBtn);

        VBox formCard = new VBox(8,
                new Label("Question"),
                formRow1,
                formRow2,
                new Label("Accepted answers"),
                formRow3,
                formRow4);
        formCard.getStyleClass().add("card");

        HBox filterRow = new HBox(8, new Label("Filter by difficulty:"), difficultyFilter, refreshBtn, backBtn);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(15, title, subtitle, filterRow, table, formCard, status);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("root");

        Scene scene = new Scene(root, 920, 680);
        Theme.apply(scene);
        stage.setScene(scene);
        stage.setTitle("Trivz — Admin");
        stage.show();

        refresh("any");
    }

    private TableView<Map<String, Object>> buildTable() {
        TableView<Map<String, Object>> table = new TableView<>();
        TableColumn<Map<String, Object>, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().get("id"))));
        TableColumn<Map<String, Object>, String> promptCol = new TableColumn<>("Prompt");
        promptCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().get("prompt"))));
        TableColumn<Map<String, Object>, String> diffCol = new TableColumn<>("Difficulty");
        diffCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().get("difficulty"))));
        TableColumn<Map<String, Object>, String> answersCol = new TableColumn<>("Answers");
        answersCol.setCellValueFactory(c -> {
            Object a = c.getValue().get("acceptedAnswers");
            if (a instanceof List<?> list) {
                return new SimpleStringProperty(String.join(", ", list.stream().map(String::valueOf).toList()));
            }
            return new SimpleStringProperty("");
        });
        table.getColumns().add(idCol);
        table.getColumns().add(promptCol);
        table.getColumns().add(diffCol);
        table.getColumns().add(answersCol);

        idCol.setPrefWidth(50);
        promptCol.setPrefWidth(380);
        diffCol.setPrefWidth(100);
        answersCol.setPrefWidth(320);
        return table;
    }

    private void refresh(String difficulty) {
        String path = ("any".equalsIgnoreCase(difficulty) || difficulty == null || difficulty.isBlank())
                ? "/api/questions"
                : "/api/questions?difficulty=" + difficulty;
        runAsync(() -> ApiClient.get(path, new TypeReference<List<Map<String, Object>>>() {}),
                list -> {
                    questions.setAll(list);
                    status.setText("Loaded " + list.size() + " questions");
                });
    }

    private void save(Long id, String prompt, String difficulty, String answersText,
                      ChoiceBox<String> filterAfter, TableView<Map<String, Object>> table) {
        if (prompt == null || prompt.isBlank()) {
            status.setText("Prompt required");
            return;
        }
        List<String> answers = (answersText == null || answersText.isBlank())
                ? List.of()
                : Arrays.stream(answersText.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (answers.isEmpty()) {
            status.setText("At least one accepted answer required");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("prompt", prompt.trim());
        body.put("difficulty", difficulty);
        body.put("acceptedAnswers", answers);

        runAsync(() -> {
            if (id == null) return ApiClient.post("/api/questions", body, Map.class, true);
            return ApiClient.put("/api/questions/" + id, body, Map.class);
        }, saved -> {
            status.setText(id == null ? "Added question" : "Updated question " + id);
            table.getSelectionModel().clearSelection();
            refresh(filterAfter.getValue());
        });
    }

    private void deleteQuestion(Long id, ChoiceBox<String> filterAfter, TableView<Map<String, Object>> table) {
        runAsync(() -> {
            ApiClient.delete("/api/questions/" + id);
            return id;
        }, removedId -> {
            status.setText("Deleted question " + removedId);
            table.getSelectionModel().clearSelection();
            refresh(filterAfter.getValue());
        });
    }

    private <T> void runAsync(Async.SafeCallable<T> work, java.util.function.Consumer<T> onOk) {
        status.setText("...");
        Async.run("trivz-admin", work, onOk, status::setText);
    }
}
