package com.mytaskmanager.services.analytics;

import com.mytaskmanager.domain.AnalyticsResult;
import com.mytaskmanager.services.fileIo.SnapshotService;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

/**
 * Runnable that performs one analytics tick and delivers the result to the provided callback.
 * Also triggers snapshot checks on each tick via {@link SnapshotService}.
 * The caller is responsible for dispatching the callback onto the appropriate thread
 * (e.g. Platform.runLater for UI updates).
 */
@RequiredArgsConstructor
public class AnalyticsRunnable implements Runnable {

    private final AnalyticsService analyticsService;
    private final SnapshotService snapshotService;
    private final Consumer<AnalyticsResult> onResult;

    @Override
    public void run() {
        try {
            snapshotService.checkAndTrigger(analyticsService.getCurrentSnapshot());
            AnalyticsResult result = analyticsService.tick();
            onResult.accept(result);
        } catch (Exception e) {
            // Prevent ScheduledExecutorService from silently cancelling all future ticks
            e.printStackTrace();
        }
    }
}
