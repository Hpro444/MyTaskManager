package com.mytaskmanager.domain;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

/**
 * Holds raw CPU and RAM metrics extracted from a single OS process snapshot.
 * Used internally by ProcessScanTask to pass data from extraction to model construction.
 */

@Getter
@Setter
@AllArgsConstructor
public class ProcessMetrics {

    private final String name;
    private final double ramPercent;
    private final double cpuPercent;
    private final int pid;
    private final long startTime;

}
