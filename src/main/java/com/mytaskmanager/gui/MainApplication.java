package com.mytaskmanager.gui;

import com.mytaskmanager.config.AppConfig;
import com.mytaskmanager.services.ScanScheduler;
import com.mytaskmanager.services.scanner.ProcessScanRunnable;
import com.mytaskmanager.services.scanner.ProcessScannerService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApplication extends Application {

    private static Stage primaryStage;
    private static MainChartView mainView;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        AppConfig config = new AppConfig();
        ProcessScannerService scannerService = new ProcessScannerService();
        mainView = new MainChartView();

        ProcessScanRunnable scanTask = new ProcessScanRunnable(scannerService, result -> Platform.runLater(() -> mainView.updateProcesses(result)));

        ScanScheduler scheduler = new ScanScheduler(config);
        scheduler.start(scanTask);

        stage.setOnCloseRequest(e -> scheduler.shutdown());

        Scene scene = new Scene(mainView, 1100, 680);
        scene.getStylesheets().add(Objects.requireNonNull(MainApplication.class.getResource("styles.css")).toExternalForm());

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

    public static void showMain() {
        primaryStage.getScene().setRoot(mainView);
    }
}
