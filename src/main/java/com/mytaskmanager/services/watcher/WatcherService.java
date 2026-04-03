package com.mytaskmanager.services.watcher;

import com.mytaskmanager.config.AppConfig;
import com.mytaskmanager.domain.ProcessInfoEntry;
import com.mytaskmanager.utils.JsonProcessInfoReader;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/**
 * Core watcher logic — owns the NIO WatchService and knows how to detect
 * and parse a change in process_info.json.
 * <p>
 * Call {@link #start(WatcherRunnable)} to register the path and launch the
 * provided runnable on a daemon thread. Call {@link #stop()} for clean shutdown.
 * </p>
 */
public class WatcherService {

    private final Path watchFilePath;
    private volatile WatchService watchService;
    private Thread watchThread;

    public WatcherService(AppConfig config) {
        // toAbsolutePath() ensures getParent() is never null for relative paths
        this.watchFilePath = Path.of(config.getMappingFilePath()).toAbsolutePath();
    }

    public synchronized void start(WatcherRunnable runnable) throws IOException {
        if (watchThread != null && watchThread.isAlive()) {
            return;
        }

        Path dir = watchFilePath.getParent();
        watchService = FileSystems.getDefault().newWatchService();
        dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        watchThread = new Thread(runnable, "watcher-thread");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    /**
     * Reads and returns the current contents of process_info.json immediately.
     * Used for the initial load on startup.
     */
    public List<ProcessInfoEntry> readCurrent() throws IOException {
        return JsonProcessInfoReader.readAll(watchFilePath.toString());
    }

    /**
     * Blocks until process_info.json is modified, then returns the parsed entries.
     * Skips events for other files in the same directory and loops back to wait.
     * Must be called from a background thread — never the JavaFX Application Thread.
     *
     * @throws InterruptedException        if the thread is interrupted while waiting
     * @throws ClosedWatchServiceException if {@link #stop()} closed the service
     * @throws IOException                 if the file cannot be read after a change event
     */
    public List<ProcessInfoEntry> awaitNextChange() throws InterruptedException, IOException {
        WatchService service = watchService;
        if (service == null) {
            throw new IllegalStateException("WatcherService.start(...) must be called before awaitNextChange().");
        }

        while (true) {
            WatchKey key = service.take(); // blocks until an event arrives
            List<ProcessInfoEntry> result = null;

            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                Path changed = (Path) event.context();
                if (changed.getFileName().equals(watchFilePath.getFileName())) {
                    result = JsonProcessInfoReader.readAll(watchFilePath.toString());
                }
            }
            key.reset();

            if (result != null) return result; // skip events for unrelated files
        }
    }

    public synchronized void stop() {
        WatchService localWatchService = watchService;
        Thread localWatchThread = watchThread;
        watchService = null;
        watchThread = null;

        if (localWatchService != null) {
            try {
                localWatchService.close();
            } catch (IOException ignored) {
            }
        }
        if (localWatchThread != null) localWatchThread.interrupt();
    }

}
