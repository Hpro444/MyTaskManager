package com.mytaskmanager.gui;

import com.mytaskmanager.domain.ProcessModel;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class ProcessDetailView extends BorderPane {

    private final List<ProcessModel> allProcesses;

    // Detail panel labels (updated when table row is clicked)
    private final Label processNameLabel = new Label();
    private final Label totalTimeLabel   = new Label();
    private final Label ramValueLabel    = new Label();
    private final Label ramRankLabel     = new Label();
    private final Label cpuValueLabel    = new Label();
    private final Label cpuRankLabel     = new Label();

    private final TableView<ProcessModel> processTable;

    public ProcessDetailView(ProcessModel selected, List<ProcessModel> allProcesses) {
        this.allProcesses = allProcesses;
        this.processTable = buildProcessTable(selected);

        setTop(buildHeader());
        setCenter(buildCenter());

        populateDetailPanel(selected);
    }

    // -------------------------------------------------------------------------
    // Header: back button + toolbar
    // -------------------------------------------------------------------------

    private HBox buildHeader() {
        Button backBtn = new Button("\u2190 Back to Main Chart View");
        backBtn.getStyleClass().add("back-button");
        backBtn.setOnAction(e -> MainApplication.show(new MainChartView()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

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
        toolbar.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        HBox header = new HBox(10, backBtn, spacer, toolbar);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    // -------------------------------------------------------------------------
    // Center: table left, detail panel right
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
        // Style labels
        processNameLabel.getStyleClass().add("detail-process-name");
        processNameLabel.setWrapText(true);

        totalTimeLabel.getStyleClass().add("detail-time");

        Label resourceHeader = new Label("Resource Usage");
        resourceHeader.getStyleClass().add("detail-subsection");

        ramValueLabel.getStyleClass().add("metric-value");
        ramRankLabel.getStyleClass().add("metric-rank");
        cpuValueLabel.getStyleClass().add("metric-value");
        cpuRankLabel.getStyleClass().add("metric-rank");

        HBox ramRow = new HBox(4, ramValueLabel, ramRankLabel);
        ramRow.setAlignment(Pos.CENTER_LEFT);

        HBox cpuRow = new HBox(4, cpuValueLabel, cpuRankLabel);
        cpuRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep1 = new Separator();
        Separator sep2 = new Separator();

        Label actionsHeader = new Label("Process Actions");
        actionsHeader.getStyleClass().add("detail-subsection");

        HBox topActions    = buildActionButtons();
        HBox bottomActions = buildActionButtons2();

        VBox pane = new VBox(10,
                processNameLabel,
                totalTimeLabel,
                sep1,
                resourceHeader,
                ramRow,
                cpuRow,
                sep2,
                actionsHeader,
                topActions,
                bottomActions
        );
        pane.getStyleClass().addAll("card", "detail-panel");
        pane.setPrefWidth(520);
        return pane;
    }

    private HBox buildActionButtons() {
        Button killBtn   = new Button("Kill Process");
        Button nameBtn   = new Button("Change Name");
        killBtn.getStyleClass().addAll("action-button", "danger");
        nameBtn.getStyleClass().addAll("action-button", "neutral");
        killBtn.setOnAction(e -> showStub("Kill Process"));
        nameBtn.setOnAction(e -> showStub("Change Name"));

        HBox row = new HBox(12, killBtn, nameBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildActionButtons2() {
        Button freezeBtn    = new Button("Freeze Tracking");
        Button categoryBtn  = new Button("Change Category");
        freezeBtn.getStyleClass().addAll("action-button", "warning");
        categoryBtn.getStyleClass().addAll("action-button", "neutral");
        freezeBtn.setOnAction(e -> showStub("Freeze Tracking"));
        categoryBtn.setOnAction(e -> showStub("Change Category"));

        HBox row = new HBox(12, freezeBtn, categoryBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // -------------------------------------------------------------------------
    // Process table
    // -------------------------------------------------------------------------

    private TableView<ProcessModel> buildProcessTable(ProcessModel selected) {
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

        // Pre-select the passed process
        table.getSelectionModel().select(selected);

        // Row click: update detail panel in-place (no navigation)
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateDetailPanel(newVal);
            }
        });

        return table;
    }

    // -------------------------------------------------------------------------
    // Populate detail panel
    // -------------------------------------------------------------------------

    private void populateDetailPanel(ProcessModel p) {
        processNameLabel.setText(p.getName());
        totalTimeLabel.setText("Total tracked time: " + p.getFormattedTime());
        ramValueLabel.setText(String.format("RAM: %.1f%%", p.getRamUsagePercent()));
        ramRankLabel.setText("  (" + ordinal(p.getRamRank() + 1) + " on RAM usage)");
        cpuValueLabel.setText(String.format("CPU: %.1f%%", p.getCpuUsagePercent()));
        cpuRankLabel.setText("  (" + ordinal(p.getCpuRank() + 1) + " on CPU usage)");
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static String ordinal(int n) {
        String[] suffixes = {"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        int mod100 = n % 100;
        return n + (mod100 >= 11 && mod100 <= 13 ? "th" : suffixes[n % 10]);
    }

    private void showStub(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Feature Stub");
        alert.setHeaderText(feature);
        alert.setContentText(feature + ": coming soon.");
        alert.showAndWait();
    }
}
