package com.mytaskmanager.gui;

import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessModel;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MainChartView extends BorderPane {

    private final ConcurrentHashMap<Integer, ProcessModel> allProcesses = new ConcurrentHashMap<>();

    // Flat sorted list shared with ProcessDetailView for its own live table
    private final ObservableList<ProcessModel> flatItems = FXCollections.observableArrayList();

    // Tree root (hidden) — children are group rows
    private final TreeItem<ProcessModel> treeRoot = new TreeItem<>();
    private final TreeTableView<ProcessModel> processTable;

    public MainChartView() {
        this.processTable = buildProcessTable();
        setTop(buildToolBar());
        setCenter(buildCenter());
    }

    /**
     * Called on the JavaFX Application Thread after each scan.
     * Updates existing ProcessModel instances in-place, then rebuilds the grouped tree.
     */
    public void updateProcesses(ConcurrentHashMap<Integer, ProcessModel> newScan) {
        Set<Integer> newPids = newScan.keySet();

        // Remove gone processes
        allProcesses.keySet().retainAll(newPids);

        // Update existing models in-place; add new ones
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
            }
        }

        // Rebuild the flat list for ProcessDetailView
        flatItems.setAll(allProcesses.values().stream().sorted(Comparator.comparingInt(ProcessModel::getCpuRank)).collect(Collectors.toList()));
        rebuildTree();
    }

    /**
     * Rebuilds the TreeTableView by grouping allProcesses by name.
     * Single-instance processes appear as flat rows; multi-instance groups are collapsible.
     * Expansion state is preserved across rebuilds.
     */
    private void rebuildTree() {
        TreeItem<ProcessModel> previousSelection = processTable.getSelectionModel().getSelectedItem();
        int previousPid = Integer.MIN_VALUE;
        String previousGroupName = null;
        if (previousSelection != null && previousSelection.getValue() != null) {
            ProcessModel selectedModel = previousSelection.getValue();
            if (selectedModel.getPid() >= 0) {
                previousPid = selectedModel.getPid();
            } else if (selectedModel.getPid() == -1) {
                previousGroupName = selectedModel.getName();
            }
        }

        // Preserve which group names are currently expanded
        Set<String> expanded = treeRoot.getChildren().stream().filter(TreeItem::isExpanded).map(item -> item.getValue().getName()).collect(Collectors.toSet());

        // Group by process name
        Map<String, List<ProcessModel>> byName = allProcesses.values().stream().collect(Collectors.groupingBy(ProcessModel::getName));

        List<TreeItem<ProcessModel>> groupItems = new ArrayList<>();

        for (Map.Entry<String, List<ProcessModel>> entry : byName.entrySet()) {
            String name = entry.getKey();
            List<ProcessModel> instances = entry.getValue();

            TreeItem<ProcessModel> groupItem;

            if (instances.size() == 1) {
                groupItem = new TreeItem<>(instances.getFirst());
            } else {
                // Aggregate CPU/RAM across all instances for the group row
                double totalCpu = Math.min(100.0, instances.stream().mapToDouble(ProcessModel::getCpuUsagePercent).sum());
                double totalRam = Math.min(100.0, instances.stream().mapToDouble(ProcessModel::getRamUsagePercent).sum());
                int bestCpuRank = instances.stream().mapToInt(ProcessModel::getCpuRank).min().orElse(1);
                int bestRamRank = instances.stream().mapToInt(ProcessModel::getRamRank).min().orElse(1);

                ProcessModel aggregate = new ProcessModel(name, Category.UNCATEGORIZED, 0L, totalRam, totalCpu, bestRamRank, bestCpuRank, -1, 0L);

                groupItem = new TreeItem<>(aggregate);
                groupItem.setExpanded(expanded.contains(name));

                instances.stream().sorted(Comparator.comparingDouble(ProcessModel::getCpuUsagePercent).reversed()).forEach(inst -> groupItem.getChildren().add(new TreeItem<>(inst)));
            }

            groupItems.add(groupItem);
        }

        // Sort groups by CPU usage descending (highest at top)
        groupItems.sort(Comparator.comparingDouble((TreeItem<ProcessModel> item) -> item.getValue().getCpuUsagePercent()).reversed());

        treeRoot.getChildren().setAll(groupItems);

        // Keep the user's current selection stable across live updates.
        if (previousPid != Integer.MIN_VALUE || previousGroupName != null) {
            TreeItem<ProcessModel> restored = null;
            for (TreeItem<ProcessModel> group : treeRoot.getChildren()) {
                ProcessModel groupModel = group.getValue();
                if (groupModel == null) continue;

                if (previousPid != Integer.MIN_VALUE) {
                    if (groupModel.getPid() == previousPid) {
                        restored = group;
                        break;
                    }
                    for (TreeItem<ProcessModel> child : group.getChildren()) {
                        ProcessModel childModel = child.getValue();
                        if (childModel != null && childModel.getPid() == previousPid) {
                            restored = child;
                            break;
                        }
                    }
                    if (restored != null) break;
                }

                if (previousGroupName != null && groupModel.getPid() == -1 && previousGroupName.equals(groupModel.getName())) {
                    restored = group;
                    break;
                }
            }

            if (restored != null) {
                processTable.getSelectionModel().select(restored);
            }
        }
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

    private TreeTableView<ProcessModel> buildProcessTable() {
        TreeTableView<ProcessModel> table = new TreeTableView<>(treeRoot);
        table.setShowRoot(false);
        table.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Scanning..."));

        // Open details only on explicit user click, never on scan-driven selection churn.
        table.setRowFactory(tv -> {
            TreeTableRow<ProcessModel> row = new TreeTableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 1) return;
                if (row.isEmpty()) return;

                ProcessModel p = row.getItem();
                if (p != null && p.getPid() >= 0)
                    MainApplication.show(new ProcessDetailView(p, flatItems));
            });
            return row;
        });

        TreeTableColumn<ProcessModel, String> nameCol = new TreeTableColumn<>("Process");
        nameCol.setCellValueFactory(d -> d.getValue().getValue().nameProperty());
        nameCol.setPrefWidth(200);
        nameCol.setSortable(false);

        TreeTableColumn<ProcessModel, String> catCol = new TreeTableColumn<>("Category");
        catCol.setCellValueFactory(d -> {
            TreeItem<ProcessModel> item = d.getValue();
            if (item == null || item.getValue() == null) {
                return new javafx.beans.property.SimpleStringProperty("");
            }

            ProcessModel model = item.getValue();
            if (model.getPid() >= 0) {
                return new javafx.beans.property.SimpleStringProperty(model.getCategoryDisplay());
            }

            Set<Category> childCategories = item.getChildren().stream()
                    .map(TreeItem::getValue)
                    .filter(Objects::nonNull)
                    .map(ProcessModel::getCategory)
                    .collect(Collectors.toSet());

            if (childCategories.isEmpty()) {
                return new javafx.beans.property.SimpleStringProperty(Category.UNCATEGORIZED.displayName());
            }
            if (childCategories.size() == 1) {
                return new javafx.beans.property.SimpleStringProperty(childCategories.iterator().next().displayName());
            }
            return new javafx.beans.property.SimpleStringProperty("Mixed");
        });
        catCol.setPrefWidth(120);
        catCol.setSortable(false);

        TreeTableColumn<ProcessModel, String> cpuCol = new TreeTableColumn<>("CPU %");
        cpuCol.setCellValueFactory(d -> Bindings.createStringBinding(() -> String.format("%.1f%%", d.getValue().getValue().getCpuUsagePercent()), d.getValue().getValue().cpuUsagePercentProperty()));
        cpuCol.setPrefWidth(70);
        cpuCol.setSortable(false);

        TreeTableColumn<ProcessModel, String> ramCol = new TreeTableColumn<>("RAM %");
        ramCol.setCellValueFactory(d -> Bindings.createStringBinding(() -> String.format("%.1f%%", d.getValue().getValue().getRamUsagePercent()), d.getValue().getValue().ramUsagePercentProperty()));
        ramCol.setPrefWidth(70);
        ramCol.setSortable(false);

        table.getColumns().addAll(nameCol, catCol, cpuCol, ramCol);


        return table;
    }


    private PieChart buildPieChart() {
        Map<Category, Long> totals = allProcesses.values().stream().collect(Collectors.groupingBy(ProcessModel::getCategory, Collectors.summingLong(ProcessModel::getTotalSeconds)));

        List<PieChart.Data> data = new ArrayList<>();

        for (Category cat : Category.values()) {
            long seconds = totals.getOrDefault(cat, 0L);
            if (seconds > 0) data.add(new PieChart.Data(cat.displayName(), seconds));
        }

        PieChart chart = new PieChart(FXCollections.observableArrayList(data));
        chart.setTitle("Time by Category");
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setPrefHeight(260);
        return chart;
    }

    private VBox buildStatsSection() {
        Map<Category, Long> totals = allProcesses.values().stream().collect(Collectors.groupingBy(ProcessModel::getCategory, Collectors.summingLong(ProcessModel::getTotalSeconds)));

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
        detailsBtn.setOnAction(e -> MainApplication.show(new CategoryView(category, new ArrayList<>(allProcesses.values()))));

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
