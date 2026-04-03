package com.mytaskmanager.domain;

import java.util.List;
import java.util.Map;

public record AnalyticsResult(
        Map<Category, Long> timeByCategory,
        List<ProcessModel> top10ByTime
) {}
