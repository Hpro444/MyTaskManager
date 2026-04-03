package com.mytaskmanager.services.analytics;

import com.mytaskmanager.config.AppConfig;
import com.mytaskmanager.domain.AnalyticsResult;
import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessModel;
import com.mytaskmanager.services.fileIo.FileIoScheduler;
import com.mytaskmanager.services.fileIo.FileIoService;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Pure analytics logic — no scheduling or threading.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Aggregate tracked time per category and identify Top 10 processes by time.</li>
 *   <li>Trigger periodic CSV snapshots (every {@code snapshot.interval} seconds).</li>
 *   <li>Trigger fixed-time CSV snapshots at times listed in {@code config.properties}.</li>
 * </ul>
 * <p>
 * Thread safety: the process list is published from the FX thread via {@link #publishSnapshot}
 * using an {@link AtomicReference}. The analytics thread reads it lock-free via {@link #tick()}.
 */
public class AnalyticsService {

    private final AtomicReference<List<ProcessModel>> snapshot = new AtomicReference<>(List.of());

    private final long snapshotIntervalSeconds;
    private final List<LocalTime> fixedTimes;
    private final FileIoScheduler fileIoScheduler;
    private final FileIoService fileIoService;
    private final String snapshotDirectory = "snapshot";

    private volatile Instant lastPeriodicSnapshot = Instant.now();

    // Tracks which fixed times have fired this "pass" to prevent double-fire within the same second
    private final Set<String> firedThisSecond = ConcurrentHashMap.newKeySet();

    public AnalyticsService(AppConfig config, FileIoScheduler fileIoScheduler, FileIoService fileIoService) {
        this.snapshotIntervalSeconds = config.getSnapshotIntervalSeconds();
        this.fixedTimes = config.getSnapshotFixedTimes();
        this.fileIoScheduler = fileIoScheduler;
        this.fileIoService = fileIoService;
    }

    /**
     * Called on the JavaFX Application Thread after every process scan update.
     * Creates an immutable copy of the process list for the analytics thread to consume safely.
     */
    public void publishSnapshot(Collection<ProcessModel> processes) {
        snapshot.set(List.copyOf(processes));
    }

    /**
     * Performs one analytics tick: checks snapshot triggers and computes the current result.
     * Called by {@link AnalyticsRunnable} on the analytics thread.
     */
    public AnalyticsResult tick() {
        List<ProcessModel> current = snapshot.get();
        checkFixedTimeSnapshots(current);
        checkPeriodicSnapshot(current);
        return compute(current);
    }

    private void checkFixedTimeSnapshots(List<ProcessModel> current) {
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
        for (LocalTime fixed : fixedTimes) {
            String key = fixed.toString();
            if (now.equals(fixed)) {
                if (firedThisSecond.add(key)) {
                    fileIoScheduler.submit(() -> fileIoService.writeCsvSnapshot(current, snapshotDirectory));
                }
            } else {
                firedThisSecond.remove(key);
            }
        }
    }

    private void checkPeriodicSnapshot(List<ProcessModel> current) {
        long elapsed = Duration.between(lastPeriodicSnapshot, Instant.now()).getSeconds();
        if (elapsed >= snapshotIntervalSeconds) {
            lastPeriodicSnapshot = Instant.now();
            fileIoScheduler.submit(() -> fileIoService.writeCsvSnapshot(current, snapshotDirectory));
        }
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
