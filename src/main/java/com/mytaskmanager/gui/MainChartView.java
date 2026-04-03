package com.mytaskmanager.gui;

import com.mytaskmanager.domain.AnalyticsResult;
import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessInfoEntry;
import com.mytaskmanager.domain.ProcessModel;
import com.mytaskmanager.services.analytics.AnalyticsService;
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

/**
 * Main UI view displaying all running processes in a tree table and category time in a pie chart.
 * <p>
 * Manages the process list lifecycle: receives scans from ProcessScannerService, updates existing models
 * in-place, detects PID reuse, and rebuilds the grouped tree structure. Provides entry points for analytics
 * updates, mapping changes, and process list exports. Handles user interactions like row selection and
 * category detail navigation.
 * </p>
 */
public class MainChartView extends BorderPane {

    private final Map<Integer, ProcessModel> allProcesses = new HashMap<>();

    // Flat sorted list shared with ProcessDetailView for its own live table
    private final ObservableList<ProcessModel> flatItems = FXCollections.observableArrayList();

    // Tree root (hidden) — children are group rows
    private final TreeItem<ProcessModel> treeRoot = new TreeItem<>();
    private final TreeTableView<ProcessModel> processTable;
    private final PieChart pieChart;
    private final Map<Category, Label> statLabels = new EnumMap<>(Category.class);

    // Injected by MainApplication after construction
    private AnalyticsService analyticsService;

    public MainChartView() {
        this.processTable = buildProcessTable();
        this.pieChart = buildPieChart();
        setTop(buildToolBar());
        setCenter(buildCenter());
    }

    /**
     * Called by MainApplication after construction to wire in services.
     */
    public void setAnalyticsService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Called on the JavaFX Application Thread after each scan.
     * Updates existing ProcessModel instances in-place, then rebuilds the grouped tree.
     */
    public void updateProcesses(ConcurrentHashMap<Integer, ProcessModel> newScan) {
        Set<Integer> newPids = newScan.keySet();

        // Remove terminated processes
        allProcesses.keySet().retainAll(newPids);

        // Update existing models in-place; add new ones; detect PID reuse via startTime
        for (Map.Entry<Integer, ProcessModel> entry : newScan.entrySet()) {
            int pid = entry.getKey();
            ProcessModel fresh = entry.getValue();
            ProcessModel existing = allProcesses.get(pid);
            if (existing != null) {
                // PID reuse detection: if startTime changed, this is a different process
                if (existing.getStartTime() != fresh.getStartTime()) {
                    allProcesses.put(pid, fresh);
                } else {
                    if (!existing.isTrackingFreezed()) {
                        existing.ramUsagePercentProperty().set(fresh.getRamUsagePercent());
                        existing.cpuUsagePercentProperty().set(fresh.getCpuUsagePercent());
                        existing.ramRankProperty().set(fresh.getRamRank());
                        existing.cpuRankProperty().set(fresh.getCpuRank());
                        existing.totalSecondsProperty().set(existing.getTotalSeconds() + fresh.getTotalSeconds());
                    }
                }
            } else {
                allProcesses.put(pid, fresh);
            }
        }

        assignRanks();
        flatItems.setAll(allProcesses.values().stream()
                .sorted(Comparator.comparingInt(ProcessModel::getCpuRank))
                .collect(Collectors.toList()));
        rebuildTree();

        // Publish immutable snapshot for the analytics thread
        if (analyticsService != null) {
            analyticsService.publishSnapshot(allProcesses.values());
        }
    }

    /**
     * Called on the JavaFX Application Thread when process_info.json is modified externally.
     * Applies category, alias, and freeze-state changes to all matching ProcessModels by name.
     */
    public void applyMappingUpdates(List<ProcessInfoEntry> entries) {
        Map<String, ProcessInfoEntry> byName = new HashMap<>();
        for (ProcessInfoEntry e : entries) byName.put(e.originalName(), e);

        for (ProcessModel model : allProcesses.values()) {
            ProcessInfoEntry entry = byName.get(model.getName());
            if (entry == null) continue;
            model.categoryProperty().set(entry.category());
            model.aliasNameProperty().set(entry.aliasName());
            model.isTrackingFreezedProperty().set(entry.isTrackingFrozen());
            model.totalSecondsProperty().set(entry.totalTimeSeconds());
        }
        updatePieChartAndStats();
    }

    // Last totals delivered to the pie chart — used to skip redraws with no visible proportion change
    private final Map<Category, Long> lastPieTotals = new EnumMap<>(Category.class);

    /**
     * Called on the JavaFX Application Thread with analytics results from the background thread.
     * Only redraws the pie chart when a slice's share of the total shifts by ≥ 0.5%.
     */
    public void applyAnalytics(AnalyticsResult result) {
        Map<Category, Long> totals = result.timeByCategory();

        if (proportionsChanged(totals)) {
            lastPieTotals.clear();
            lastPieTotals.putAll(totals);

            // Always add every category in enum order so CSS data-index colors are stable
            List<PieChart.Data> slices = new ArrayList<>();
            for (Category cat : Category.values()) {
                slices.add(new PieChart.Data(cat.displayName(), totals.getOrDefault(cat, 0L)));
            }
            pieChart.getData().setAll(slices);
        }

        for (Category cat : Category.values()) {
            Label label = statLabels.get(cat);
            if (label != null)
                label.setText(cat.displayName() + " — " + formatSeconds(totals.getOrDefault(cat, 0L)));
        }
    }

    private boolean proportionsChanged(Map<Category, Long> totals) {
        long newTotal = totals.values().stream().mapToLong(Long::longValue).sum();
        long oldTotal = lastPieTotals.values().stream().mapToLong(Long::longValue).sum();
        if (newTotal == 0 && oldTotal == 0) return false;
        if (newTotal == 0 || oldTotal == 0) return true;

        for (Category cat : Category.values()) {
            double newPct = 100.0 * totals.getOrDefault(cat, 0L) / newTotal;
            double oldPct = 100.0 * lastPieTotals.getOrDefault(cat, 0L) / oldTotal;
            if (Math.abs(newPct - oldPct) >= 0.5) return true;
        }
        return false;
    }

    /**
     * Builds process entries grouped by name for Save/Shutdown operations.
     * Must be called on the FX thread (reads allProcesses).
     */
    List<ProcessInfoEntry> buildEntries() {
        Map<String, List<ProcessModel>> byName = allProcesses.values().stream().collect(Collectors.groupingBy(ProcessModel::getName));

        return byName.entrySet().stream().map(e -> {
            ProcessModel rep = e.getValue().getFirst();
            long totalSecs = e.getValue().stream().mapToLong(ProcessModel::getTotalSeconds).max().orElse(0L);
            return new ProcessInfoEntry(rep.getName(), rep.getAliasName(),
                    rep.getCategory(), rep.isTrackingFreezed(), totalSecs);
        }).collect(Collectors.toList());
    }

    private void updatePieChartAndStats() {
        Map<Category, Long> totals = allProcesses.values().stream().collect(Collectors.groupingBy(ProcessModel::getCategory, Collectors.summingLong(ProcessModel::getTotalSeconds)));

        List<PieChart.Data> data = new ArrayList<>();
        for (Category cat : Category.values()) {
            long seconds = totals.getOrDefault(cat, 0L);
            if (seconds > 0) data.add(new PieChart.Data(cat.displayName(), seconds));
        }
        pieChart.getData().setAll(data);

        for (Category cat : Category.values()) {
            Label label = statLabels.get(cat);
            if (label != null)
                label.setText(cat.displayName() + " — " + formatSeconds(totals.getOrDefault(cat, 0L)));
        }
    }

    /**
     * Assigns RAM and CPU ranks to allProcesses. Must run on the FX Application Thread.
     */
    private void assignRanks() {
        List<ProcessModel> byRam = allProcesses.values().stream().filter(p -> !p.isTrackingFreezed()).collect(Collectors.toList());
        byRam.sort(Comparator.comparingDouble(ProcessModel::getRamUsagePercent).reversed());
        for (int i = 0; i < byRam.size(); i++) byRam.get(i).ramRankProperty().set(i + 1);

        List<ProcessModel> byCpu = allProcesses.values().stream().filter(p -> !p.isTrackingFreezed()).collect(Collectors.toList());
        byCpu.sort(Comparator.comparingDouble(ProcessModel::getCpuUsagePercent).reversed());
        for (int i = 0; i < byCpu.size(); i++) byCpu.get(i).cpuRankProperty().set(i + 1);
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

        Set<String> expanded = treeRoot.getChildren().stream().filter(TreeItem::isExpanded).map(item -> item.getValue().getName()).collect(Collectors.toSet());

        Map<String, List<ProcessModel>> byName = allProcesses.values().stream().collect(Collectors.groupingBy(ProcessModel::getName));

        List<TreeItem<ProcessModel>> groupItems = new ArrayList<>();

        for (Map.Entry<String, List<ProcessModel>> entry : byName.entrySet()) {
            String name = entry.getKey();
            List<ProcessModel> instances = entry.getValue();

            TreeItem<ProcessModel> groupItem;

            if (instances.size() == 1) {
                groupItem = new TreeItem<>(instances.getFirst());
            } else {
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

        groupItems.sort(Comparator.comparingDouble((TreeItem<ProcessModel> item) -> item.getValue().getCpuUsagePercent()).reversed());

        treeRoot.getChildren().setAll(groupItems);

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

    /**
     * Builds the top toolbar with Save, Load, and Shutdown buttons.
     */
    private HBox buildToolBar() {
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

        HBox header = new HBox(toolbar);
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
     * Builds the left pane containing the process tree table.
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
     * Builds the right pane containing the pie chart and statistics.
     */
    private VBox buildRightPane() {
        VBox statsSection = buildStatsSection();

        VBox.setVgrow(pieChart, Priority.ALWAYS);

        VBox pane = new VBox(12, pieChart, statsSection);
        pane.setPadding(new Insets(12));
        pane.getStyleClass().add("card");
        pane.setPrefWidth(520);
        return pane;
    }

    /**
     * Builds the process tree table with columns for name, category, CPU, and RAM.
     */
    private TreeTableView<ProcessModel> buildProcessTable() {
        TreeTableView<ProcessModel> table = new TreeTableView<>(treeRoot);
        table.setShowRoot(false);
        table.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Scanning..."));

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
        nameCol.setCellValueFactory(d -> d.getValue().getValue().aliasNameProperty());
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

            Set<Category> childCategories = item.getChildren().stream().map(TreeItem::getValue).filter(Objects::nonNull).map(ProcessModel::getCategory).collect(Collectors.toSet());

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
        ramCol.setCellValueFactory(d -> Bindings.createStringBinding(
                () -> String.format("%.1f%%", d.getValue().getValue().getRamUsagePercent()),
                d.getValue().getValue().ramUsagePercentProperty()));
        ramCol.setPrefWidth(70);
        ramCol.setSortable(false);

        table.getColumns().addAll(nameCol, catCol, cpuCol, ramCol);
        return table;
    }

    /**
     * Builds the pie chart for time by category.
     */
    private PieChart buildPieChart() {
        PieChart chart = new PieChart(FXCollections.observableArrayList());
        chart.setTitle("Time by Category");
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setPrefHeight(260);
        return chart;
    }

    /**
     * Builds the statistics section with category labels and detail buttons.
     */
    private VBox buildStatsSection() {
        VBox section = new VBox(4);
        for (Category cat : Category.values())
            section.getChildren().add(buildStatRow(cat));
        return section;
    }

    /**
     * Builds a single statistics row for a category.
     */
    private HBox buildStatRow(Category category) {
        Label label = new Label(category.displayName() + " — 0h 0m");
        label.getStyleClass().add("stats-label");
        statLabels.put(category, label);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button detailsBtn = new Button("Details");
        detailsBtn.getStyleClass().add("details-button");
        detailsBtn.setOnAction(e -> MainApplication.showCategory(category));

        HBox row = new HBox(8, label, spacer, detailsBtn);
        row.getStyleClass().add("stats-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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
        long sec = s % 60;
        return String.format("%dh %dm", h, m);
    }
}

