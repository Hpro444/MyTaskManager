package com.mytaskmanager.services.fileIo;

import com.mytaskmanager.config.AppConfig;
import com.mytaskmanager.domain.ProcessModel;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides when to write CSV snapshots and submits the work to {@link FileIoScheduler}.
 * Supports two trigger modes:
 * <ul>
 *   <li>Periodic — every {@code snapshot.interval} seconds.</li>
 *   <li>Fixed-time — at specific times listed in {@code config.properties}.</li>
 * </ul>
 * Called once per analytics tick via {@link #checkAndTrigger}.
 */
public class SnapshotService {

    private final long snapshotIntervalSeconds;
    private final List<LocalTime> fixedTimes;
    private final FileIoScheduler fileIoScheduler;
    private final FileIoService fileIoService;
    private final String snapshotDirectory = "snapshot";

    private volatile Instant lastPeriodicSnapshot = Instant.now();

    // Tracks which fixed times have fired this "pass" to prevent double-fire within the same second
    private final Set<String> firedThisSecond = ConcurrentHashMap.newKeySet();

    public SnapshotService(AppConfig config, FileIoScheduler fileIoScheduler, FileIoService fileIoService) {
        this.snapshotIntervalSeconds = config.getSnapshotIntervalSeconds();
        this.fixedTimes = config.getSnapshotFixedTimes();
        this.fileIoScheduler = fileIoScheduler;
        this.fileIoService = fileIoService;
    }

    /**
     * Checks both trigger conditions and submits a CSV write if either fires.
     * Called on the analytics thread once per second.
     */
    public void checkAndTrigger(List<ProcessModel> processes) {
        checkFixedTimeSnapshots(processes);
        checkPeriodicSnapshot(processes);
    }

    private void checkFixedTimeSnapshots(List<ProcessModel> processes) {
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
        for (LocalTime fixed : fixedTimes) {
            String key = fixed.toString();
            if (now.equals(fixed)) {
                if (firedThisSecond.add(key)) {
                    fileIoScheduler.submit(() -> fileIoService.writeCsvSnapshot(processes, snapshotDirectory));
                }
            } else {
                firedThisSecond.remove(key);
            }
        }
    }

    private void checkPeriodicSnapshot(List<ProcessModel> processes) {
        long elapsed = Duration.between(lastPeriodicSnapshot, Instant.now()).getSeconds();
        if (elapsed >= snapshotIntervalSeconds) {
            lastPeriodicSnapshot = Instant.now();
            fileIoScheduler.submit(() -> fileIoService.writeCsvSnapshot(processes, snapshotDirectory));
        }
    }
}
