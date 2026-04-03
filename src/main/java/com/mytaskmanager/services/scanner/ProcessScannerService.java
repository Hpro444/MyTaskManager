package com.mytaskmanager.services.scanner;

import com.mytaskmanager.domain.ProcessModel;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private Map<Integer, Long> lastSeenTimestampMs = new HashMap<>();

    public ProcessScannerService() {
        this.systemInfo = new SystemInfo();
        this.operatingSystem = systemInfo.getOperatingSystem();
    }

    /**
     * Scans all active OS processes in parallel and returns performance snapshots keyed by PID.
     * Must be called from a background thread — never the JavaFX Application Thread.
     *
     * @return map of PID -> {@link ProcessModel}; empty if no processes found
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

        long nowMs = System.currentTimeMillis();

        ConcurrentHashMap<Integer, ProcessModel> scannedProcesses;
        try (ForkJoinPool scanPool = new ForkJoinPool()) {
            ProcessScannerTask scanTask = new ProcessScannerTask(currentList, 0, currentList.size(), totalSystemMemoryBytes, logicalProcessorCount, priorSnapshot);
            scannedProcesses = scanPool.invoke(scanTask);
        }

        // Stamp each fresh model with the seconds elapsed since this PID was last scanned.
        // updateProcesses() on the FX thread will add this delta to the live model's totalSeconds.
        for (ProcessModel model : scannedProcesses.values()) {
            Long lastMs = lastSeenTimestampMs.get(model.getPid());
            if (lastMs != null)
                model.totalSecondsProperty().set((nowMs - lastMs) / 1000);
            lastSeenTimestampMs.put(model.getPid(), nowMs);
        }
        lastSeenTimestampMs.keySet().retainAll(scannedProcesses.keySet());

        return scannedProcesses;
    }
}
