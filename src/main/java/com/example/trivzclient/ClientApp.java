package com.example.trivzclient;

import com.example.trivzclient.screen.LoginScreen;
import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Trivz");
        new LoginScreen(stage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
