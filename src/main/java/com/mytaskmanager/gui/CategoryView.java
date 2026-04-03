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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Detail view for a specific category, showing all processes in that category and top 10 by time.
 * <p>
 * Filters the full process snapshot by the selected category, displays statistics and a pie chart
 * of the top 10 processes in that category. Updated on every analytics tick with the latest snapshot.
 * </p>
 */
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

    /**
     * Returns the currently displayed category.
     *
     * @return the current category, or null if not set
     */
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

    /**
     * Builds the header with back button and action buttons.
     */
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

    /**
     * Builds the center container with process list and pie chart.
     */
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

    /**
     * Builds the left pane with the process list table.
     */
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

    /**
     * Builds the process table with Name, RAM/CPU, and Time columns.
     *
     * @return configured TableView
     */
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

    /**
     * Builds the right pane with the top 10 pie chart.
     */
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

    /**
     * Builds the footer with total time information.
     */
    private HBox buildFooter() {
        HBox footer = new HBox(footerLabel);
        footer.getStyleClass().add("footer-bar");
        footer.setAlignment(Pos.CENTER_LEFT);
        return footer;
    }

    /**
     * Checks if the pie chart data has changed significantly.
     *
     * @param newTotals the new totals map
     * @return true if proportions changed by >= 0.5%
     */
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

    /**
     * Formats seconds into a human-readable time string (e.g., "5h 30m").
     *
     * @param s total seconds
     * @return formatted time string
     */
    private static String formatSeconds(long s) {
        long h = s / 3600;
        long m = (s % 3600) / 60;
        return String.format("%dh %dm", h, m);
    }
}
