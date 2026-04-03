package com.mytaskmanager.services.fileIo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    public void submit(Runnable task) {
        executor.submit(new FileIoRunnable(task));
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
