package com.mytaskmanager.domain;

/**
 * Holds raw CPU and RAM metrics extracted from a single OS process snapshot.
 * Used internally by ProcessScanTask to pass data from extraction to model construction.
 */

public record ProcessMetrics(String name, double ramPercent, double cpuPercent, int pid, long startTime) {

}
