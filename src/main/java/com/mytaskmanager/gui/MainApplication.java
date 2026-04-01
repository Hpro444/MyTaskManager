package com.mytaskmanager.gui;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        MainChartView mainView = new MainChartView();
        Scene scene = new Scene(mainView, 1100, 680);
        scene.getStylesheets().add(
                MainApplication.class.getResource("styles.css").toExternalForm()
        );

        stage.setTitle("Productivity Buddy");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void show(Parent view) {
        primaryStage.getScene().setRoot(view);
    }
}
