package com.mytaskmanager.domain;

import javafx.beans.property.*;
import lombok.Getter;
import lombok.ToString;
import lombok.AccessLevel;

/**
 * Domain model representing an OS process with performance metrics.
 * <p>
 * Uses JavaFX properties for observable data binding in UI components.
 * Lombok provides getters and toString methods automatically.
 * </p>
 */
@Getter
@ToString
public class ProcessModel {

    @Getter(AccessLevel.NONE)
    private final StringProperty name;
    @Getter(AccessLevel.NONE)
    private final ObjectProperty<Category> category;
    @Getter(AccessLevel.NONE)
    private final LongProperty totalSeconds;
    @Getter(AccessLevel.NONE)
    private final DoubleProperty ramUsagePercent;
    @Getter(AccessLevel.NONE)
    private final DoubleProperty cpuUsagePercent;
    @Getter(AccessLevel.NONE)
    private final IntegerProperty ramRank;
    @Getter(AccessLevel.NONE)
    private final IntegerProperty cpuRank;

    public ProcessModel(String name, Category category, long totalSeconds, double ramUsagePercent, double cpuUsagePercent,int ramRank, int cpuRank) {
        this.name = new SimpleStringProperty(name);
        this.category = new SimpleObjectProperty<>(category);
        this.totalSeconds = new SimpleLongProperty(totalSeconds);
        this.ramUsagePercent = new SimpleDoubleProperty(ramUsagePercent);
        this.cpuUsagePercent = new SimpleDoubleProperty(cpuUsagePercent);
        this.ramRank = new SimpleIntegerProperty(ramRank);
        this.cpuRank = new SimpleIntegerProperty(cpuRank);
    }

    // Property accessors for JavaFX binding
    public StringProperty nameProperty() { return name; }
    public ObjectProperty<Category> categoryProperty() { return category; }
    public LongProperty totalSecondsProperty() { return totalSeconds; }
    public DoubleProperty ramUsagePercentProperty() { return ramUsagePercent; }
    public DoubleProperty cpuUsagePercentProperty() { return cpuUsagePercent; }
    public IntegerProperty ramRankProperty() { return ramRank; }
    public IntegerProperty cpuRankProperty() { return cpuRank; }

    // Value getters
    public String getName() { return name.get(); }
    public Category getCategory() { return category.get(); }
    public long getTotalSeconds() { return totalSeconds.get(); }
    public double getRamUsagePercent() { return ramUsagePercent.get(); }
    public double getCpuUsagePercent() { return cpuUsagePercent.get(); }
    public int getRamRank() { return ramRank.get(); }
    public int getCpuRank() { return cpuRank.get(); }

    public String getFormattedTime() {
        long s = totalSeconds.get();
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%dh %dm %ds", h, m, sec);
    }

    public String getRamAndCpu() {
        return String.format("%.1f%% / %.1f%%", ramUsagePercent.get(), cpuUsagePercent.get());
    }

    public String getCategoryDisplay() {
        return category.get().displayName();
    }
}
