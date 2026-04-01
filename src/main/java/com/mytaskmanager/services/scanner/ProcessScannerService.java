package com.mytaskmanager.services.scanner;

import com.mytaskmanager.domain.ProcessModel;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * Service responsible for scanning active OS processes in parallel.
 * <p>
 * Uses a ForkJoinPool with divide-and-conquer (via {@link ProcessScanTask}) to leverage
 * multi-core processors. Each scan captures RAM and CPU usage metrics and assigns
 * performance ranks (1 = highest resource consumer) across all returned processes.
 * </p>
 * <p>
 * Category and session time are left at defaults; callers should populate
 * these from the process registry and process_info.json as needed.
 * </p>
 */
public class ProcessScannerService {

    private final SystemInfo systemInfo;
    private final OperatingSystem operatingSystem;

    public ProcessScannerService() {
        this.systemInfo = new SystemInfo();
        this.operatingSystem = systemInfo.getOperatingSystem();
    }

    /**
     * Scans all active OS processes in parallel and returns performance snapshots.
     * <p>
     * Must be called from a background thread — never the JavaFX Application Thread.
     * </p>
     *
     * @return unmodifiable list of {@link ProcessModel} with ranks assigned; empty if no processes found
     */
    public List<ProcessModel> scanProcesses() {
        long totalSystemMemoryBytes = systemInfo.getHardware().getMemory().getTotal();
        int logicalProcessorCount = systemInfo.getHardware().getProcessor().getLogicalProcessorCount();

        List<OSProcess> osProcessSnapshot = operatingSystem.getProcesses();
        if (osProcessSnapshot.isEmpty())
            return List.of();

        List<ProcessModel> scannedProcesses;
        try (ForkJoinPool scanPool = new ForkJoinPool()) {
            ProcessScanTask scanTask = new ProcessScanTask(osProcessSnapshot, 0, osProcessSnapshot.size(), totalSystemMemoryBytes, logicalProcessorCount);
            scannedProcesses = scanPool.invoke(scanTask);
        }

        assignRanks(scannedProcesses);
        return Collections.unmodifiableList(scannedProcesses);
    }

    /**
     * Assigns RAM and CPU ranks to all processes (rank 1 = highest consumer).
     * Runs single-threaded after the pool completes — safe to mutate JavaFX properties here.
     */
    private void assignRanks(List<ProcessModel> processes) {
        List<ProcessModel> byRamDescending = new ArrayList<>(processes);
        byRamDescending.sort(Comparator.comparingDouble(ProcessModel::getRamUsagePercent).reversed());

        for (int rank = 0; rank < byRamDescending.size(); rank++)
            byRamDescending.get(rank).ramRankProperty().set(rank + 1);

        List<ProcessModel> byCpuDescending = new ArrayList<>(processes);
        byCpuDescending.sort(Comparator.comparingDouble(ProcessModel::getCpuUsagePercent).reversed());

        for (int rank = 0; rank < byCpuDescending.size(); rank++)
            byCpuDescending.get(rank).cpuRankProperty().set(rank + 1);
    }
}
