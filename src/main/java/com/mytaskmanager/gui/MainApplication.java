package com.mytaskmanager.gui;

import com.mytaskmanager.config.AppConfig;
import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessInfoEntry;
import com.mytaskmanager.services.ScanScheduler;
import com.mytaskmanager.services.analytics.AnalyticsRunnable;
import com.mytaskmanager.services.analytics.AnalyticsScheduler;
import com.mytaskmanager.services.analytics.AnalyticsService;
import com.mytaskmanager.services.fileIo.FileIoScheduler;
import com.mytaskmanager.services.fileIo.FileIoService;
import com.mytaskmanager.services.scanner.ProcessScannerRunnable;
import com.mytaskmanager.services.scanner.ProcessScannerService;
import com.mytaskmanager.services.snapshot.SnapshotRunnable;
import com.mytaskmanager.services.snapshot.SnapshotScheduler;
import com.mytaskmanager.services.snapshot.SnapshotService;
import com.mytaskmanager.services.watcher.WatcherRunnable;
import com.mytaskmanager.services.watcher.WatcherService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main JavaFX Application class and orchestrator for all background services.
 * <p>
 * Initializes and wires all core services (scanning, analytics, snapshots, file I/O, watching),
 * manages the UI views (main chart and category detail), and handles the graceful shutdown sequence.
 * Provides static accessors for UI views to interact with services and perform actions like
 * save, load, and view navigation.
 * </p>
 */
public class MainApplication extends Application {

    private static Stage primaryStage;
    private static MainChartView mainView;
    private static CategoryView categoryView;

    // Services shared across all views via static accessors
    private static AppConfig config;
    private static FileIoService fileIoService;
    private static FileIoScheduler fileIoScheduler;
    private static ScanScheduler scanScheduler;
    private static AnalyticsScheduler analyticsScheduler;
    private static AnalyticsService analyticsService;
    private static SnapshotScheduler snapshotScheduler;
    private static WatcherService watcherService;
    private static final AtomicBoolean shutdownStarted = new AtomicBoolean(false);

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        config = new AppConfig();
        fileIoService = new FileIoService();
        fileIoScheduler = new FileIoScheduler();
        ProcessScannerService scannerService = new ProcessScannerService();
        analyticsService = new AnalyticsService();
        SnapshotService snapshotService = new SnapshotService(config, fileIoScheduler, fileIoService);

        mainView = new MainChartView();
        mainView.setAnalyticsService(analyticsService);
        categoryView = new CategoryView();

        // Wire analytics results back to the UI on the FX thread
        AnalyticsRunnable analyticsRunnable = new AnalyticsRunnable(analyticsService,
                result -> Platform.runLater(() -> {
                    mainView.applyAnalytics(result);
                    Category cat = categoryView.getCurrentCategory();
                    if (cat != null)
                        categoryView.applyUpdate(cat, analyticsService.getCurrentSnapshot());
                }));
        analyticsScheduler = new AnalyticsScheduler();
        analyticsScheduler.start(analyticsRunnable);

        SnapshotRunnable snapshotRunnable = new SnapshotRunnable(analyticsService::getCurrentSnapshot, snapshotService);
        snapshotScheduler = new SnapshotScheduler();
        snapshotScheduler.start(snapshotRunnable, config.getSnapshotSchedulerDelaySeconds());

        watcherService = new WatcherService(config);
        WatcherRunnable watcherRunnable = new WatcherRunnable(watcherService, entries -> Platform.runLater(() -> mainView.applyMappingUpdates(entries)));

        // Start watcher only after the first scan so allProcesses is populated
        // when the initial readCurrent() fires inside WatcherRunnable.run()
        boolean[] firstScan = {true};
        ProcessScannerRunnable scanTask = new ProcessScannerRunnable(scannerService, result ->
                Platform.runLater(() -> {
                    mainView.updateProcesses(result);
                    if (firstScan[0]) {
                        firstScan[0] = false;
                        try {
                            watcherService.start(watcherRunnable);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }));

        scanScheduler = new ScanScheduler(config);
        scanScheduler.start(scanTask);

        stage.setOnCloseRequest(e -> {
            e.consume(); // prevent immediate close — shutdown must sync data first
            performShutdown();
        });

        Scene scene = new Scene(mainView, 1100, 680);
        scene.getStylesheets().add(Objects.requireNonNull(
                MainApplication.class.getResource("styles.css")).toExternalForm());

        stage.setTitle("Productivity Buddy");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    /**
     * Displays the specified parent node in the primary stage.
     *
     * @param view the Parent node to display
     */
    public static void show(Parent view) {
        primaryStage.getScene().setRoot(view);
    }

    /**
     * Displays the main chart view.
     */
    public static void showMain() {
        primaryStage.getScene().setRoot(mainView);
    }

    /**
     * Displays the category detail view for the specified category.
     *
     * @param category the category to display details for
     */
    public static void showCategory(Category category) {
        categoryView.applyUpdate(category, analyticsService.getCurrentSnapshot());
        primaryStage.getScene().setRoot(categoryView);
    }

    /**
     * Opens a FileChooser and saves the current mapping to a user-chosen JSON file.
     */
    public static void save() {
        if (shutdownStarted.get()) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Mapping");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = fc.showSaveDialog(primaryStage);
        if (file == null) return;
        List<ProcessInfoEntry> entries = mainView.buildEntries();
        fileIoScheduler.submit(() -> fileIoService.save(entries, file.getAbsolutePath()));
    }

    /**
     * Opens a FileChooser and loads a previously saved mapping file.
     */
    public static void load() {
        if (shutdownStarted.get()) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Load Mapping");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = fc.showOpenDialog(primaryStage);
        if (file == null) return;
        fileIoScheduler.submit(() -> {
            List<ProcessInfoEntry> entries = fileIoService.load(file.getAbsolutePath());
            fileIoService.save(entries, config.getMappingFilePath());
            Platform.runLater(() -> mainView.applyMappingUpdates(entries));
        });
    }

    /**
     * Graceful shutdown sequence:
     * 1. Stop all background threads (no shutdownNow).
     * 2. Sync accumulated time into process_info.json.
     * 3. Wait for the file write to complete before exiting.
     */
    public static void performShutdown() {
        if (!shutdownStarted.compareAndSet(false, true)) {
            return;
        }

        scanScheduler.shutdown();
        analyticsScheduler.shutdown();
        snapshotScheduler.shutdown();
        watcherService.stop();

        CountDownLatch fxDrainLatch = new CountDownLatch(1);
        AtomicReference<List<ProcessInfoEntry>> entriesRef = new AtomicReference<>(List.of());

        // Queue this after any in-flight runLater UI updates, then capture a final FX-owned snapshot.
        Platform.runLater(() -> {
            entriesRef.set(mainView.buildEntries());
            fxDrainLatch.countDown();
        });

        // Wait off the FX thread, then persist and exit.
        new Thread(() -> {
            try {
                fxDrainLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            fileIoScheduler.submit(() -> fileIoService.save(entriesRef.get(), config.getMappingFilePath()));
            fileIoScheduler.shutdownAndAwait();
            Platform.exit();
        }, "shutdown-await").start();
    }
}
