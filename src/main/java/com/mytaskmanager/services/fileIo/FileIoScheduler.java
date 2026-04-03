package com.mytaskmanager.services.fileIo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Single-thread executor for all file I/O operations.
 * Serial execution guarantees that concurrent Save + auto-snapshot requests
 * never corrupt the same file.
 */
public class FileIoScheduler {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "file-io-thread");
        t.setDaemon(true);
        return t;
    });

    /**
     * Submits a task to the file I/O executor.
     * Silently ignores submissions during shutdown without throwing exceptions.
     *
     * @param task the task to execute
     */
    public void submit(Runnable task) {
        try {
            executor.submit(new FileIoRunnable(task));
        } catch (RejectedExecutionException ignored) {
            // Safe no-op during shutdown races (e.g., duplicate UI actions while exiting).
        }
    }

    /**
     * Gracefully shuts down the executor and waits up to 10 seconds for pending tasks.
     * Must be called from a background thread — never the FX thread.
     */
    public void shutdownAndAwait() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
