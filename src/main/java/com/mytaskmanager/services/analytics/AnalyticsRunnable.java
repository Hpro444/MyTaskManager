package com.mytaskmanager.services.analytics;

import com.mytaskmanager.domain.AnalyticsResult;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

/**
 * Runnable that performs one analytics tick and delivers the result to the provided callback.
 * The caller is responsible for dispatching the callback onto the appropriate thread
 * (e.g. Platform.runLater for UI updates).
 */
@RequiredArgsConstructor
public class AnalyticsRunnable implements Runnable {

    private final AnalyticsService analyticsService;
    private final Consumer<AnalyticsResult> onResult;

    /**
     * Executes one analytics tick and invokes the callback with results.
     * Catches exceptions to prevent the scheduler from silently cancelling future ticks.
     */
    @Override
    public void run() {
        try {
            AnalyticsResult result = analyticsService.tick();
            onResult.accept(result);
        } catch (Exception e) {
            // Prevent ScheduledExecutorService from silently cancelling all future ticks
            e.printStackTrace();
        }
    }
}
