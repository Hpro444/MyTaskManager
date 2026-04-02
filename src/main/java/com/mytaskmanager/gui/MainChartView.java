package com.mytaskmanager.gui;

import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MainChartView extends BorderPane {

    private final ConcurrentHashMap<Integer, ProcessModel> allProcesses = new ConcurrentHashMap<>();
    @Getter
    private final ObservableList<ProcessModel> tableItems = FXCollections.observableArrayList();
    private final TableView<ProcessModel> processTable;

    public MainChartView() {
        this.processTable = buildProcessTable();
        setTop(buildToolBar());
        setCenter(buildCenter());
    }

    /**
     * Called on the JavaFX Application Thread (via Platform.runLater) after each scan completes.
     * Updates existing ProcessModel instances in-place so JavaFX property bindings auto-refresh,
     * adds new processes, removes gone ones, and re-sorts the table.
     */
    public void updateProcesses(ConcurrentHashMap<Integer, ProcessModel> newScan) {
        Set<Integer> newPids = newScan.keySet();

        // Remove processes that are no longer running
        allProcesses.keySet().retainAll(newPids);
        tableItems.removeIf(p -> !newPids.contains(p.getPid()));

        // Update existing models in-place; add truly new processes
        for (Map.Entry<Integer, ProcessModel> entry : newScan.entrySet()) {
            ProcessModel fresh = entry.getValue();
            ProcessModel existing = allProcesses.get(entry.getKey());
            if (existing != null) {
                existing.ramUsagePercentProperty().set(fresh.getRamUsagePercent());
                existing.cpuUsagePercentProperty().set(fresh.getCpuUsagePercent());
                existing.ramRankProperty().set(fresh.getRamRank());
                existing.cpuRankProperty().set(fresh.getCpuRank());
            } else {
                allProcesses.put(entry.getKey(), fresh);
                tableItems.add(fresh);
            }
        }

        // Re-sort in place — preserves TableView selection
        tableItems.sort(Comparator.comparingInt(ProcessModel::getCpuRank));
    }


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

        // Disable sorting on columns
        nameCol.setSortable(false);
        catCol.setSortable(false);

        table.getColumns().addAll(nameCol, catCol);
        table.setItems(tableItems);
        table.setPlaceholder(new Label("Scanning..."));

        table.getSortOrder().clear();
        table.setSortPolicy(tv -> false);

        // Row click: navigate to Process Detail View
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                MainApplication.show(new ProcessDetailView(selected, tableItems));
            }
        });

        return table;
    }


    private PieChart buildPieChart() {
        Map<Category, Long> totals = allProcesses.values().stream()
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


    private VBox buildStatsSection() {
        Map<Category, Long> totals = allProcesses.values().stream()
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
        detailsBtn.setOnAction(e -> MainApplication.show(
                new CategoryView(category, new ArrayList<>(allProcesses.values()))));

        HBox row = new HBox(8, label, spacer, detailsBtn);
        row.getStyleClass().add("stats-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

   
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
