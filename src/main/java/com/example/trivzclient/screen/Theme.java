package com.example.trivzclient.screen;

import javafx.scene.Scene;

import java.net.URL;

public class Theme {

    public static void apply(Scene scene) {
        URL css = Theme.class.getResource("/trivz.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }
}
