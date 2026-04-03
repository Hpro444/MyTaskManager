package com.mytaskmanager.gui;

import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessSnapshot;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.stream.Collectors;

public class CategoryView extends BorderPane {

    private Category currentCategory;

    private final Label titleLabel;
    private final ObservableList<ProcessSnapshot> tableItems = FXCollections.observableArrayList();
    private final PieChart topChart;
    private final Label footerLabel;

    // Last top-10 snapshot delivered to the pie — key: aliasName, value: totalSeconds
    private final Map<String, Long> lastPieTotals = new LinkedHashMap<>();

    public CategoryView() {
        titleLabel = new Label();
        titleLabel.getStyleClass().add("view-title");

        topChart = new PieChart(FXCollections.observableArrayList());
        topChart.setTitle("Top 10 by Time");
        topChart.setLabelsVisible(true);
        topChart.setLegendVisible(false);
        topChart.setPrefHeight(320);

        footerLabel = new Label();
        footerLabel.getStyleClass().add("footer-total");

        setTop(buildHeader());
        setCenter(buildCenter());
        setBottom(buildFooter());
    }

    public Category getCurrentCategory() {
        return currentCategory;
    }

    /**
     * Updates all UI components for the given category using the latest process snapshot.
     * Safe to call on every analytics tick.
     */
    public void applyUpdate(Category category, List<ProcessSnapshot> allProcesses) {
        if (category != currentCategory) lastPieTotals.clear();
        currentCategory = category;
        titleLabel.setText(category.displayName() + " Category");

        List<ProcessSnapshot> filtered = allProcesses.stream()
                .filter(p -> p.category() == category)
                .collect(Collectors.toList());

        tableItems.setAll(filtered);

        List<ProcessSnapshot> top10 = filtered.stream()
                .sorted(Comparator.comparingLong(ProcessSnapshot::totalSeconds).reversed())
                .limit(10)
                .toList();
        Map<String, Long> newPieTotals = new LinkedHashMap<>();
        top10.forEach(p -> newPieTotals.put(p.aliasName(), p.totalSeconds()));

        if (pieChanged(newPieTotals)) {
            lastPieTotals.clear();
            lastPieTotals.putAll(newPieTotals);
            List<PieChart.Data> data = top10.stream()
                    .map(p -> new PieChart.Data(p.aliasName(), p.totalSeconds()))
                    .collect(Collectors.toList());
            topChart.getData().setAll(data);
            topChart.setTitle(data.isEmpty() ? "No data" : "Top 10 by Time");
        }

        long totalSeconds = filtered.stream().mapToLong(ProcessSnapshot::totalSeconds).sum();
        footerLabel.setText(category.displayName().toLowerCase() + " total time — " + formatSeconds(totalSeconds));
    }

    private HBox buildHeader() {
        Button backBtn = new Button("← Back to Main Chart View");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> MainApplication.showMain());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button saveBtn = new Button("Save");
        Button loadBtn = new Button("Load");
        Button shutdownBtn = new Button("Shutdown");
        saveBtn.getStyleClass().add("button");
        loadBtn.getStyleClass().add("button");
        shutdownBtn.getStyleClass().addAll("button", "danger");
        saveBtn.setOnAction(e -> MainApplication.save());
        loadBtn.setOnAction(e -> MainApplication.load());
        shutdownBtn.setOnAction(e -> MainApplication.performShutdown());

        ToolBar toolbar = new ToolBar(saveBtn, loadBtn, new Separator(), shutdownBtn);
        toolbar.getStyleClass().add("tool-bar");
        toolbar.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        HBox header = new HBox(10, backBtn, titleLabel, spacer, toolbar);
        header.getStyleClass().add("header-bar");
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
        Label title = new Label("Processes");
        title.getStyleClass().add("section-title");

        TableView<ProcessSnapshot> table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox pane = new VBox(10, title, table);
        pane.setPadding(new Insets(12));
        pane.getStyleClass().add("card");
        pane.setPrefWidth(480);
        return pane;
    }

    private TableView<ProcessSnapshot> buildTable() {
        TableView<ProcessSnapshot> table = new TableView<>(tableItems);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ProcessSnapshot, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().aliasName()));
        nameCol.setPrefWidth(180);

        TableColumn<ProcessSnapshot, String> ramCpuCol = new TableColumn<>("RAM / CPU");
        ramCpuCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().ramAndCpu()));
        ramCpuCol.setPrefWidth(120);

        TableColumn<ProcessSnapshot, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().formattedTime()));
        timeCol.setPrefWidth(110);

        table.getColumns().addAll(nameCol, ramCpuCol, timeCol);
        table.setPlaceholder(new Label("No processes in this category"));
        return table;
    }

    private VBox buildRightPane() {
        Label title = new Label("Top 10 Processes by Time Spent");
        title.getStyleClass().add("section-title");

        VBox.setVgrow(topChart, Priority.ALWAYS);

        VBox pane = new VBox(10, title, topChart);
        pane.setPadding(new Insets(12));
        pane.getStyleClass().add("card");
        pane.setPrefWidth(520);
        return pane;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(footerLabel);
        footer.getStyleClass().add("footer-bar");
        footer.setAlignment(Pos.CENTER_LEFT);
        return footer;
    }

    private boolean pieChanged(Map<String, Long> newTotals) {
        if (!newTotals.keySet().equals(lastPieTotals.keySet())) return true;
        long newTotal = newTotals.values().stream().mapToLong(Long::longValue).sum();
        long oldTotal = lastPieTotals.values().stream().mapToLong(Long::longValue).sum();
        if (newTotal == 0 && oldTotal == 0) return false;
        if (newTotal == 0 || oldTotal == 0) return true;
        for (Map.Entry<String, Long> e : newTotals.entrySet()) {
            double newPct = 100.0 * e.getValue() / newTotal;
            double oldPct = 100.0 * lastPieTotals.getOrDefault(e.getKey(), 0L) / oldTotal;
            if (Math.abs(newPct - oldPct) >= 0.5) return true;
        }
        return false;
    }

    private static String formatSeconds(long s) {
        long h = s / 3600;
        long m = (s % 3600) / 60;
        return String.format("%dh %dm", h, m);
    }
}
