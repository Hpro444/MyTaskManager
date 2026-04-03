package com.mytaskmanager.services.analytics;

import com.mytaskmanager.domain.AnalyticsResult;
import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessModel;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Pure analytics computation — no scheduling, no threading, no IO.
 * <p>
 * The process list is published from the FX thread via {@link #publishSnapshot}
 * using an {@link AtomicReference}. The analytics thread reads it lock-free via {@link #tick()}.
 */
public class AnalyticsService {

    private final AtomicReference<List<ProcessModel>> snapshot = new AtomicReference<>(List.of());

    /**
     * Called on the JavaFX Application Thread after every process scan update.
     * Creates an immutable copy of the process list for the analytics thread to consume safely.
     */
    public void publishSnapshot(Collection<ProcessModel> processes) {
        snapshot.set(List.copyOf(processes));
    }

    /**
     * Returns the current process snapshot. Used by {@link AnalyticsRunnable}
     * to pass to other services that run alongside analytics each tick.
     */
    public List<ProcessModel> getCurrentSnapshot() {
        return snapshot.get();
    }

    /**
     * Computes analytics for the current snapshot.
     * Called by {@link AnalyticsRunnable} on the analytics thread.
     */
    public AnalyticsResult tick() {
        return compute(snapshot.get());
    }

    private AnalyticsResult compute(List<ProcessModel> current) {
        // Deduplicate by name (take max time across instances), then sum per category
        Map<Category, Long> byCategory = current.stream()
                .filter(m -> !m.isTrackingFreezed())
                .collect(Collectors.groupingBy(
                        ProcessModel::getName,
                        Collectors.maxBy(Comparator.comparingLong(ProcessModel::getTotalSeconds))))
                .values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.groupingBy(
                        ProcessModel::getCategory,
                        Collectors.summingLong(ProcessModel::getTotalSeconds)));

        List<ProcessModel> top10 = current.stream()
                .sorted(Comparator.comparingLong(ProcessModel::getTotalSeconds).reversed())
                .limit(10)
                .collect(Collectors.toList());

        return new AnalyticsResult(byCategory, top10);
    }
}
