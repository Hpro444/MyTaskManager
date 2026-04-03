package com.mytaskmanager.services.watcher;

import com.mytaskmanager.domain.ProcessInfoEntry;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Runnable that loops indefinitely, waiting for process_info.json changes and
 * delivering parsed entries to the provided callback.
 * The caller is responsible for dispatching the callback onto the appropriate
 * thread (e.g. Platform.runLater for UI updates).
 */
@RequiredArgsConstructor
public class WatcherRunnable implements Runnable {

    private final WatcherService watcherService;
    private final Consumer<List<ProcessInfoEntry>> onFileChanged;

    /**
     * Runs the main watch loop, handling initial load and change detection.
     * Catches exceptions gracefully to handle startup failures and mid-write events.
     */
    @Override
    public void run() {
        try {
            onFileChanged.accept(watcherService.readCurrent());
        } catch (IOException e) {
            e.printStackTrace(); // file missing on startup — skip initial load
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<ProcessInfoEntry> entries = watcherService.awaitNextChange();
                onFileChanged.accept(entries);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ClosedWatchServiceException ignored) {
                break; // stop() closed the WatchService — normal shutdown
            } catch (IOException e) {
                e.printStackTrace(); // file mid-write — skip this event, keep watching
            }
        }
    }
}
