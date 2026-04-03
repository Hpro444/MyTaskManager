package com.mytaskmanager.services.scanner;

import com.mytaskmanager.config.AppConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the background scan loop.
 * Uses a single daemon thread and scheduleWithFixedDelay so scans never overlap —
 * the next scan begins only after the previous one fully completes.
 */
public class ScanScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "scan-scheduler");
        t.setDaemon(true);
        return t;
    });

    private final long intervalSeconds;

    /**
     * Constructs a ScanScheduler with the interval from AppConfig.
     *
     * @param config the application configuration
     */
    public ScanScheduler(AppConfig config) {
        this.intervalSeconds = config.getMonitorIntervalSeconds();
    }

    /**
     * Starts the scan loop with the configured interval.
     * Scans are non-overlapping: the next scan begins only after the previous one completes.
     *
     * @param task the Runnable to execute repeatedly
     */
    public void start(Runnable task) {
        scheduler.scheduleWithFixedDelay(task, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * Gracefully shuts down the scan scheduler.
     */
    public void shutdown() {
        scheduler.shutdown();
    }
}
