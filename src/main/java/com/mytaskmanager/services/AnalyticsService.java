package com.mytaskmanager.services;

import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessModel;

import java.util.List;
import java.util.Map;

public class AnalyticsService {
    public Map<Category, Long> getTotalSecondsByCategory(List<ProcessModel> processes) {
        return Map.of(); // stub — real aggregation to be implemented
    }

    public List<ProcessModel> getTopByTime(List<ProcessModel> processes, int limit) {
        return List.of(); // stub — returns top N processes by totalSeconds
    }
}
