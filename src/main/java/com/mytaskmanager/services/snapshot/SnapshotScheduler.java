package com.mytaskmanager.services.snapshot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the background snapshot check loop.
 * Uses a single daemon thread and scheduleWithFixedDelay so checks never overlap.
 */
public class SnapshotScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "snapshot-scheduler");
        t.setDaemon(true);
        return t;
    });

    /**
     * Starts the snapshot check loop with the specified delay.
     * Checks are non-overlapping: the next check begins only after the previous one completes.
     *
     * @param task         the Runnable to execute repeatedly
     * @param delaySeconds the delay between checks in seconds
     */
    public void start(Runnable task, long delaySeconds) {
        scheduler.scheduleWithFixedDelay(task, 0, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Gracefully shuts down the snapshot scheduler.
     */
    public void shutdown() {
        scheduler.shutdown();
    }
}
