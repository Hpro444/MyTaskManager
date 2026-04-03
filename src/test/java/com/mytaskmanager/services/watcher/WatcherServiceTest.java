package com.mytaskmanager.services.watcher;

import com.mytaskmanager.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class WatcherServiceTest {

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

    @Test
    void awaitNextChange_withoutStart_throwsIllegalStateException() {
        WatcherService watcherService = new WatcherService(new AppConfig());

        assertThrows(IllegalStateException.class, watcherService::awaitNextChange);
    }

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



