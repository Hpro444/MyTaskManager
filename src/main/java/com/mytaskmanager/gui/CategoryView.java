package com.mytaskmanager.gui;

import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessModel;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CategoryView extends BorderPane {

    public CategoryView(Category category, List<ProcessModel> allProcesses) {
        List<ProcessModel> filtered = allProcesses.stream()
                .filter(p -> p.getCategory() == category)
                .collect(Collectors.toList());

        long totalSeconds = filtered.stream()
                .mapToLong(ProcessModel::getTotalSeconds)
                .sum();

        setTop(buildHeader(category));
        setCenter(buildCenter(filtered));
        setBottom(buildFooter(category, totalSeconds));
    }

    // -------------------------------------------------------------------------
    // Header: back button + title + toolbar
    // -------------------------------------------------------------------------

    private HBox buildHeader(Category category) {
        Button backBtn = new Button("\u2190 Back to Main Chart View");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> MainApplication.show(new MainChartView()));

        Label titleLabel = new Label(category.displayName() + " Category");
        titleLabel.getStyleClass().add("view-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button saveBtn     = new Button("Save");
        Button loadBtn     = new Button("Load");
        Button shutdownBtn = new Button("Shutdown");
        saveBtn.getStyleClass().add("button");
        loadBtn.getStyleClass().add("button");
        shutdownBtn.getStyleClass().addAll("button", "danger");
        saveBtn.setOnAction(e -> showStub("Save"));
        loadBtn.setOnAction(e -> showStub("Load"));
        shutdownBtn.setOnAction(e -> javafx.application.Platform.exit());

        ToolBar toolbar = new ToolBar(saveBtn, loadBtn, new Separator(), shutdownBtn);
        toolbar.getStyleClass().add("tool-bar");
        toolbar.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        HBox header = new HBox(10, backBtn, titleLabel, spacer, toolbar);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // -------------------------------------------------------------------------
    // Center: filtered table left, top-10 pie chart right
    // -------------------------------------------------------------------------

    private HBox buildCenter(List<ProcessModel> filtered) {
        VBox leftPane  = buildLeftPane(filtered);
        VBox rightPane = buildRightPane(filtered);

        HBox.setHgrow(leftPane, Priority.ALWAYS);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        HBox center = new HBox(16, leftPane, rightPane);
        center.setPadding(new Insets(16));
        center.setFillHeight(true);
        return center;
    }

    private VBox buildLeftPane(List<ProcessModel> filtered) {
        Label title = new Label("Processes");
        title.getStyleClass().add("section-title");

        TableView<ProcessModel> table = buildTable(filtered);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox pane = new VBox(10, title, table);
        pane.setPadding(new Insets(12));
        pane.getStyleClass().add("card");
        pane.setPrefWidth(480);
        return pane;
    }

    private TableView<ProcessModel> buildTable(List<ProcessModel> filtered) {
        TableView<ProcessModel> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ProcessModel, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> d.getValue().nameProperty());
        nameCol.setPrefWidth(180);

        TableColumn<ProcessModel, String> ramCpuCol = new TableColumn<>("RAM / CPU");
        ramCpuCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRamAndCpu()));
        ramCpuCol.setPrefWidth(120);

        TableColumn<ProcessModel, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getFormattedTime()));
        timeCol.setPrefWidth(110);

        table.getColumns().addAll(nameCol, ramCpuCol, timeCol);
        table.setItems(FXCollections.observableArrayList(filtered));
        table.setPlaceholder(new Label("No processes in this category"));
        return table;
    }

    private VBox buildRightPane(List<ProcessModel> filtered) {
        Label title = new Label("Top 10 Processes by Time Spent");
        title.getStyleClass().add("section-title");

        PieChart chart = buildTopChart(filtered);
        VBox.setVgrow(chart, Priority.ALWAYS);

        VBox pane = new VBox(10, title, chart);
        pane.setPadding(new Insets(12));
        pane.getStyleClass().add("card");
        pane.setPrefWidth(520);
        return pane;
    }

    private PieChart buildTopChart(List<ProcessModel> filtered) {
        List<ProcessModel> top10 = filtered.stream()
                .sorted(Comparator.comparingLong(ProcessModel::getTotalSeconds).reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<PieChart.Data> data = top10.stream()
                .map(p -> new PieChart.Data(p.getName(), p.getTotalSeconds()))
                .collect(Collectors.toList());

        PieChart chart = new PieChart(FXCollections.observableArrayList(data));
        chart.setTitle("Top 10 by Time");
        chart.setLabelsVisible(true);
        chart.setLegendVisible(false);
        chart.setPrefHeight(320);

        if (data.isEmpty()) {
            chart.setTitle("No data");
        }

        return chart;
    }

    // -------------------------------------------------------------------------
    // Footer: total time
    // -------------------------------------------------------------------------

    private HBox buildFooter(Category category, long totalSeconds) {
        String timeStr = formatSeconds(totalSeconds);
        Label label = new Label(category.displayName().toLowerCase()
                + " total time — " + timeStr);
        label.getStyleClass().add("footer-total");

        HBox footer = new HBox(label);
        footer.getStyleClass().add("footer-bar");
        footer.setAlignment(Pos.CENTER_LEFT);
        return footer;
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
