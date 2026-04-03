package com.mytaskmanager.services.analytics;

import com.mytaskmanager.domain.AnalyticsResult;
import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessModel;
import com.mytaskmanager.domain.ProcessSnapshot;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Pure analytics computation — no scheduling, no threading, no IO.
 * <p>
 * The process list is published from the FX thread via {@link #publishSnapshot}
 * using an {@link AtomicReference}. Each {@link ProcessModel} is converted to an immutable
 * {@link ProcessSnapshot} at publish time (safe: FX thread owns the models), so the analytics
 * thread reads only immutable data lock-free via {@link #tick()}.
 */
public class AnalyticsService {

    private final AtomicReference<List<ProcessSnapshot>> snapshot = new AtomicReference<>(List.of());

    /**
     * Called on the JavaFX Application Thread after every process scan update.
     * Converts each ProcessModel to an immutable ProcessSnapshot so background threads
     * never touch JavaFX property objects.
     */
    public void publishSnapshot(Collection<ProcessModel> processes) {
        snapshot.set(processes.stream()
                .map(ProcessSnapshot::of)
                .collect(Collectors.toUnmodifiableList()));
    }

    /**
     * Returns the current process snapshot. Used by {@link AnalyticsRunnable}
     * to pass to other services that run alongside analytics each tick.
     */
    public List<ProcessSnapshot> getCurrentSnapshot() {
        return snapshot.get();
    }

    /**
     * Computes analytics for the current snapshot.
     * Called by {@link AnalyticsRunnable} on the analytics thread.
     */
    public AnalyticsResult tick() {
        return compute(snapshot.get());
    }

    private AnalyticsResult compute(List<ProcessSnapshot> current) {
        // Deduplicate by name (take max time across instances), then sum per category
        Map<Category, Long> byCategory = current.stream()
                .filter(m -> !m.trackingFreezed())
                .collect(Collectors.groupingBy(
                        ProcessSnapshot::name,
                        Collectors.maxBy(Comparator.comparingLong(ProcessSnapshot::totalSeconds))))
                .values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.groupingBy(
                        ProcessSnapshot::category,
                        Collectors.summingLong(ProcessSnapshot::totalSeconds)));

        List<ProcessSnapshot> top10 = current.stream()
                .sorted(Comparator.comparingLong(ProcessSnapshot::totalSeconds).reversed())
                .limit(10)
                .collect(Collectors.toList());

        return new AnalyticsResult(byCategory, top10);
    }
}
