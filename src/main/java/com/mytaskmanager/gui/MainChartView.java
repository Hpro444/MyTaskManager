package com.mytaskmanager.gui;

import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessModel;
import com.mytaskmanager.services.scanner.ProcessScannerService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainChartView extends BorderPane {

    private final List<ProcessModel> allProcesses;
    private final TableView<ProcessModel> processTable;

    public MainChartView() {
        ProcessScannerService scanner = new ProcessScannerService();
        this.allProcesses = new ArrayList<>(scanner.scanProcesses());
        this.processTable = buildProcessTable();

        setTop(buildToolBar());
        setCenter(buildCenter());
    }

    // -------------------------------------------------------------------------
    // Top toolbar
    // -------------------------------------------------------------------------

    private HBox buildToolBar() {
        Button saveBtn = new Button("Save");
        Button loadBtn = new Button("Load");
        Button shutdownBtn = new Button("Shutdown");

        saveBtn.getStyleClass().add("button");
        loadBtn.getStyleClass().add("button");
        shutdownBtn.getStyleClass().addAll("button", "danger");

        saveBtn.setOnAction(e -> showStub("Save"));
        loadBtn.setOnAction(e -> showStub("Load"));
        shutdownBtn.setOnAction(e -> javafx.application.Platform.exit());

        ToolBar toolbar = new ToolBar(saveBtn, loadBtn, new Separator(), shutdownBtn);
        toolbar.getStyleClass().add("tool-bar");

        HBox header = new HBox(toolbar);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // -------------------------------------------------------------------------
    // Center: table on the left, chart + stats on the right
    // -------------------------------------------------------------------------

    private HBox buildCenter() {
        VBox leftPane = buildLeftPane();
        VBox rightPane = buildRightPane();

        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        HBox center = new HBox(16, leftPane, rightPane);
        center.setPadding(new Insets(16));
        center.setFillHeight(true);
        return center;
    }

    private VBox buildLeftPane() {
        Label title = new Label("Running Processes");
        title.getStyleClass().add("section-title");

        VBox.setVgrow(processTable, Priority.ALWAYS);

        VBox pane = new VBox(10, title, processTable);
        pane.setPadding(new Insets(12));
        pane.getStyleClass().add("card");
        pane.setPrefWidth(480);
        return pane;
    }

    private VBox buildRightPane() {
        PieChart chart = buildPieChart();
        VBox statsSection = buildStatsSection();

        VBox.setVgrow(chart, Priority.ALWAYS);

        VBox pane = new VBox(12, chart, statsSection);
        pane.setPadding(new Insets(12));
        pane.getStyleClass().add("card");
        pane.setPrefWidth(520);
        return pane;
    }

    // -------------------------------------------------------------------------
    // Process table
    // -------------------------------------------------------------------------

    private TableView<ProcessModel> buildProcessTable() {
        TableView<ProcessModel> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ProcessModel, String> nameCol = new TableColumn<>("Process");
        nameCol.setCellValueFactory(d -> d.getValue().nameProperty());
        nameCol.setPrefWidth(220);

        TableColumn<ProcessModel, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getCategoryDisplay()));
        catCol.setPrefWidth(120);

        table.getColumns().addAll(nameCol, catCol);
        table.setItems(FXCollections.observableArrayList(allProcesses));
        table.setPlaceholder(new Label("No processes found"));

        // Row click: navigate to Process Detail View
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                MainApplication.show(new ProcessDetailView(selected, allProcesses));
            }
        });

        return table;
    }

    // -------------------------------------------------------------------------
    // Pie chart
    // -------------------------------------------------------------------------

    private PieChart buildPieChart() {
        Map<Category, Long> totals = allProcesses.stream()
                .collect(Collectors.groupingBy(ProcessModel::getCategory,
                        Collectors.summingLong(ProcessModel::getTotalSeconds)));

        List<PieChart.Data> data = new ArrayList<>();
        for (Category cat : Category.values()) {
            long seconds = totals.getOrDefault(cat, 0L);
            if (seconds > 0) {
                data.add(new PieChart.Data(cat.displayName(), seconds));
            }
        }

        PieChart chart = new PieChart(FXCollections.observableArrayList(data));
        chart.setTitle("Time by Category");
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setPrefHeight(260);
        return chart;
    }

    // -------------------------------------------------------------------------
    // Category stats rows
    // -------------------------------------------------------------------------

    private VBox buildStatsSection() {
        Map<Category, Long> totals = allProcesses.stream()
                .collect(Collectors.groupingBy(ProcessModel::getCategory,
                        Collectors.summingLong(ProcessModel::getTotalSeconds)));

        VBox section = new VBox(4);
        for (Category cat : Category.values()) {
            long seconds = totals.getOrDefault(cat, 0L);
            section.getChildren().add(buildStatRow(cat, seconds));
        }
        return section;
    }

    private HBox buildStatRow(Category category, long totalSeconds) {
        String timeStr = formatSeconds(totalSeconds);
        Label label = new Label(category.displayName() + " — " + timeStr);
        label.getStyleClass().add("stats-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().add("details-button");
        detailsBtn.setOnAction(e -> MainApplication.show(new CategoryView(category, allProcesses)));

        HBox row = new HBox(8, label, spacer, detailsBtn);
        row.getStyleClass().add("stats-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static String formatSeconds(long s) {
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%dh %dm %ds", h, m, sec);
    }

    private void showStub(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Feature Stub");
        alert.setHeaderText(feature);
        alert.setContentText(feature + ": coming soon.");
        alert.showAndWait();
    }
}
