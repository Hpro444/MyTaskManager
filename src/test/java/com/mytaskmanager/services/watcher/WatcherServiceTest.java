package com.mytaskmanager.services.watcher;

import com.mytaskmanager.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WatcherService}.
 * <p>
 * Verifies that the NIO watch service can be safely started multiple times (idempotent)
 * and that awaiting file changes without starting the service throws a descriptive error.
 * </p>
 */
class WatcherServiceTest {

    /**
     * Tests that start() can be called multiple times safely (idempotent).
     */
    @Test
    void start_calledTwice_isSafeAndIdempotent() throws IOException {
        WatcherService watcherService = new WatcherService(new AppConfig());
        WatcherRunnable runnable = new WatcherRunnable(watcherService, entries -> {
        });

        watcherService.start(runnable);
        assertTrue(isRunning(watcherService));

        assertDoesNotThrow(() -> watcherService.start(runnable));
        assertTrue(isRunning(watcherService));

        watcherService.stop();
        assertFalse(isRunning(watcherService));
    }

    /**
     * Tests that awaitNextChange() throws an error if start() was not called.
     */
    @Test
    void awaitNextChange_withoutStart_throwsIllegalStateException() {
        WatcherService watcherService = new WatcherService(new AppConfig());

        assertThrows(IllegalStateException.class, watcherService::awaitNextChange);
    }

    /**
     * Helper to check if the watcher thread is running using reflection.
     *
     * @param watcherService the service to check
     * @return true if the watcher thread is alive
     */
    private static boolean isRunning(WatcherService watcherService) {
        try {
            Field watchThreadField = WatcherService.class.getDeclaredField("watchThread");
            watchThreadField.setAccessible(true);
            Thread watchThread = (Thread) watchThreadField.get(watcherService);
            return watchThread != null && watchThread.isAlive();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to inspect watcher thread state", e);
        }
    }
}



