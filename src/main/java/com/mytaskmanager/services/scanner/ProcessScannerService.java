package com.mytaskmanager.services.scanner;

import com.mytaskmanager.domain.ProcessModel;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

/**
 * Service responsible for scanning active OS processes in parallel.
 * <p>
 * Retains the previous OS snapshot between calls so that CPU between-ticks
 * calculation uses the full inter-scan window — no in-scan sleep required.
 * The first scan returns 0.0 CPU for all processes (no prior snapshot yet).
 * </p>
 */
public class ProcessScannerService {

    private final SystemInfo systemInfo;
    private final OperatingSystem operatingSystem;

    private Map<Integer, OSProcess> previousSnapshot = new HashMap<>();

    public ProcessScannerService() {
        this.systemInfo = new SystemInfo();
        this.operatingSystem = systemInfo.getOperatingSystem();
    }

    /**
     * Scans all active OS processes in parallel and returns performance snapshots keyed by PID.
     * Must be called from a background thread — never the JavaFX Application Thread.
     *
     * @return map of PID -> {@link ProcessModel} with ranks assigned; empty if no processes found
     */
    public ConcurrentHashMap<Integer, ProcessModel> scanProcesses() {
        long totalSystemMemoryBytes = systemInfo.getHardware().getMemory().getTotal();
        int logicalProcessorCount = systemInfo.getHardware().getProcessor().getLogicalProcessorCount();

        List<OSProcess> currentList = operatingSystem.getProcesses();
        if (currentList.isEmpty())
            return new ConcurrentHashMap<>();

        Map<Integer, OSProcess> priorSnapshot = previousSnapshot;

        // Store current snapshot for the next scan's CPU delta calculation
        Map<Integer, OSProcess> nextSnapshot = new HashMap<>(currentList.size() * 2);
        for (OSProcess p : currentList) nextSnapshot.put(p.getProcessID(), p);
        previousSnapshot = nextSnapshot;

        ConcurrentHashMap<Integer, ProcessModel> scannedProcesses;
        try (ForkJoinPool scanPool = new ForkJoinPool()) {
            ProcessScanTask scanTask = new ProcessScanTask(currentList, 0, currentList.size(), totalSystemMemoryBytes, logicalProcessorCount, priorSnapshot);
            scannedProcesses = scanPool.invoke(scanTask);
        }

        assignRanks(scannedProcesses);
        return scannedProcesses;
    }

    /**
     * Assigns RAM and CPU ranks to all processes (rank 1 = highest consumer).
     * Runs single-threaded after the pool completes — safe to mutate JavaFX properties here.
     */
    private void assignRanks(ConcurrentHashMap<Integer, ProcessModel> processes) {
        List<ProcessModel> byRamDescending = new ArrayList<>(processes.values());
        byRamDescending.sort(Comparator.comparingDouble(ProcessModel::getRamUsagePercent).reversed());

        for (int rank = 0; rank < byRamDescending.size(); rank++)
            byRamDescending.get(rank).ramRankProperty().set(rank + 1);

        List<ProcessModel> byCpuDescending = new ArrayList<>(processes.values());
        byCpuDescending.sort(Comparator.comparingDouble(ProcessModel::getCpuUsagePercent).reversed());

        for (int rank = 0; rank < byCpuDescending.size(); rank++)
            byCpuDescending.get(rank).cpuRankProperty().set(rank + 1);
    }
}
