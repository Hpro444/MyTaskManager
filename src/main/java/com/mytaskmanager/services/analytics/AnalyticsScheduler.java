package com.mytaskmanager.services.analytics;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the background analytics loop.
 * Uses a single daemon thread and scheduleWithFixedDelay so ticks never overlap —
 * the next tick begins only after the previous one fully completes.
 */
public class AnalyticsScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "analytics-scheduler");
        t.setDaemon(true);
        return t;
    });

    public void start(Runnable task) {
        scheduler.scheduleWithFixedDelay(task, 0, 1, TimeUnit.SECONDS);
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
