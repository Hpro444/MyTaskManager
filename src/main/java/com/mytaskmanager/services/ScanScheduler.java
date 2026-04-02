package com.mytaskmanager.services;

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

    public ScanScheduler(AppConfig config) {
        this.intervalSeconds = config.getMonitorIntervalSeconds();
    }

    public void start(Runnable task) {
        scheduler.scheduleWithFixedDelay(task, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
