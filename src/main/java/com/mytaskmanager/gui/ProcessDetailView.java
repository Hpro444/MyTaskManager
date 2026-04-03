package com.mytaskmanager.gui;

import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessModel;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Detail view for a single process instance.
 * <p>
 * Displays comprehensive metrics (CPU, RAM, total time, ranks) for a selected process and allows
 * user interactions such as killing the process, changing its alias, freezing tracking, or changing
 * its category. Also shows a live list of all running processes for quick navigation.
 * </p>
 */
public class ProcessDetailView extends BorderPane {

    private final ObservableList<ProcessModel> allProcesses;
    private final ProcessModel selected;

    private final Label processNameLabel = new Label();
    private final Label totalTimeLabel = new Label();
    private final Label ramValueLabel = new Label();
    private final Label ramRankLabel = new Label();
    private final Label cpuValueLabel = new Label();
    private final Label cpuRankLabel = new Label();

    private final TableView<ProcessModel> processTable;

    /**
     * Constructs a detail view for a specific process.
     *
     * @param selected     the process to display details for
     * @param allProcesses all processes for navigation
     */
    public ProcessDetailView(ProcessModel selected, ObservableList<ProcessModel> allProcesses) {
        this.selected = selected;
        this.allProcesses = allProcesses;
        this.processTable = buildProcessTable(selected);

        setTop(buildHeader());
        setCenter(buildCenter());

        bindDetailPanel(selected);
    }

    /**
     * Builds the header with navigation and action buttons.
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

        HBox header = new HBox(10, backBtn, spacer, toolbar);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /**
     * Builds the center container with left process list and right details panel.
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
        Label title = new Label("Running Processes");
        title.getStyleClass().add("section-title");

        VBox.setVgrow(processTable, Priority.ALWAYS);

        VBox pane = new VBox(10, title, processTable);
        pane.setPadding(new Insets(12));
        pane.getStyleClass().add("card");
        pane.setPrefWidth(480);
        return pane;
    }

    /**
     * Builds the right pane with detailed metrics and action buttons.
     */
    private VBox buildRightPane() {
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

        HBox topActions = buildActionButtons();
        HBox bottomActions = buildActionButtons2();

        VBox pane = new VBox(10, processNameLabel, totalTimeLabel, sep1, resourceHeader,
                ramRow, cpuRow, sep2, actionsHeader, topActions, bottomActions);
        pane.getStyleClass().addAll("card", "detail-panel");
        pane.setPrefWidth(520);
        return pane;
    }

    /**
     * Builds the first row of action buttons (Kill Process, Change Alias).
     */
    private HBox buildActionButtons() {
        Button killBtn = new Button("Kill Process");
        Button nameBtn = new Button("Change Alias");
        killBtn.getStyleClass().addAll("action-button", "danger");
        nameBtn.getStyleClass().addAll("action-button", "neutral");

        killBtn.setOnAction(e -> {
            ProcessHandle.of(selected.getPid()).ifPresent(ProcessHandle::destroy);
            MainApplication.showMain();
        });

        nameBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog(selected.getAliasName());
            dialog.setTitle("Set Alias");
            dialog.setHeaderText("Enter alias for " + selected.getAliasName());
            dialog.showAndWait().ifPresent(alias -> allProcesses.stream().filter(p -> p.getName().equals(selected.getName())).forEach(p -> p.aliasNameProperty().set(alias)));
        });

        HBox row = new HBox(12, killBtn, nameBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * Builds the second row of action buttons (Freeze Tracking, Change Category).
     */
    private HBox buildActionButtons2() {
        Button freezeBtn = new Button(selected.isTrackingFreezed() ? "Unfreeze Tracking" : "Freeze Tracking");
        Button categoryBtn = new Button("Change Category");
        freezeBtn.getStyleClass().addAll("action-button", "warning");
        categoryBtn.getStyleClass().addAll("action-button", "neutral");

        freezeBtn.setOnAction(e -> {
            boolean nowFrozen = !selected.isTrackingFreezed();
            allProcesses.stream().filter(p -> p.getName().equals(selected.getName())).forEach(p -> p.isTrackingFreezedProperty().set(nowFrozen));
            freezeBtn.setText(nowFrozen ? "Unfreeze Tracking" : "Freeze Tracking");
        });

        categoryBtn.setOnAction(e -> {
            ChoiceDialog<Category> dialog = new ChoiceDialog<>(selected.getCategory(), Category.values());
            dialog.setTitle("Change Category");
            dialog.setHeaderText("Category for " + selected.getName());
            dialog.showAndWait().ifPresent(cat -> allProcesses.stream().filter(p -> p.getName().equals(selected.getName())).forEach(p -> p.categoryProperty().set(cat)));
        });

        HBox row = new HBox(12, freezeBtn, categoryBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * Builds the process table showing all running processes.
     *
     * @param initialSelected the process initially selected
     * @return configured TableView
     */
    private TableView<ProcessModel> buildProcessTable(ProcessModel initialSelected) {
        TableView<ProcessModel> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ProcessModel, String> nameCol = new TableColumn<>("Process");
        nameCol.setCellValueFactory(d -> d.getValue().aliasNameProperty());
        nameCol.setPrefWidth(220);

        TableColumn<ProcessModel, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getCategoryDisplay()));
        catCol.setPrefWidth(120);

        nameCol.setSortable(false);
        catCol.setSortable(false);

        table.getColumns().addAll(nameCol, catCol);
        table.setItems(allProcesses);
        table.setPlaceholder(new Label("No processes found"));
        table.getSortOrder().clear();
        table.setSortPolicy(tv -> false);

        table.getSelectionModel().select(initialSelected);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) bindDetailPanel(newVal);
        });

        return table;
    }

    /**
     * Binds the detail panel labels to the specified process's properties.
     *
     * @param p the process to bind to
     */
    private void bindDetailPanel(ProcessModel p) {
        processNameLabel.textProperty().bind(p.aliasNameProperty());
        totalTimeLabel.textProperty().bind(Bindings.createStringBinding(() -> "Total tracked time: " + p.getFormattedTime(), p.totalSecondsProperty()));
        ramValueLabel.textProperty().bind(Bindings.createStringBinding(() -> String.format("RAM: %.1f%%", p.getRamUsagePercent()), p.ramUsagePercentProperty()));
        ramRankLabel.textProperty().bind(Bindings.createStringBinding(() -> "  (" + ordinal(p.getRamRank()) + " on RAM usage)", p.ramRankProperty()));
        cpuValueLabel.textProperty().bind(Bindings.createStringBinding(() -> String.format("CPU: %.1f%%", p.getCpuUsagePercent()), p.cpuUsagePercentProperty()));
        cpuRankLabel.textProperty().bind(Bindings.createStringBinding(() -> "  (" + ordinal(p.getCpuRank()) + " on CPU usage)", p.cpuRankProperty()));
    }

    /**
     * Converts an integer rank into an ordinal suffix (1st, 2nd, 3rd, etc.).
     *
     * @param n the rank number
     * @return ordinal representation (e.g., "1st", "2nd", "3rd", "4th")
     */
    private static String ordinal(int n) {
        String[] suffixes = {"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        int mod100 = n % 100;
        return n + (mod100 >= 11 && mod100 <= 13 ? "th" : suffixes[n % 10]);
    }
}
