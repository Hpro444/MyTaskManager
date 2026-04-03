package com.mytaskmanager.domain;

import java.util.List;
import java.util.Map;

/**
 * Immutable record holding computed analytics for a snapshot in time.
 * <p>
 * Contains aggregated time by category (for the pie chart) and the top 10 processes
 * by total time (for detail views). Produced by {@link com.mytaskmanager.services.analytics.AnalyticsService}
 * and delivered to the UI for rendering.
 * </p>
 */
public record AnalyticsResult(
        Map<Category, Long> timeByCategory,
        List<ProcessSnapshot> top10ByTime
) {}
