package com.mytaskmanager.services.scanner;

import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessMetrics;
import com.mytaskmanager.domain.ProcessModel;
import lombok.RequiredArgsConstructor;
import oshi.software.os.OSProcess;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

/**
 * ForkJoinTask that uses divide-and-conquer to efficiently scan processes in parallel.
 * <p>
 * Recursively subdivides the process list until reaching the leaf threshold,
 * then processes each leaf segment sequentially. Captures RAM and CPU metrics
 * for each process, gracefully handling access denied or terminated process errors.
 * </p>
 */
@RequiredArgsConstructor
public class ProcessScanTask extends RecursiveTask<ConcurrentHashMap<Integer, ProcessModel>> {

    private static final int LEAF_THRESHOLD = 10;

    private final List<OSProcess> osProcessSnapshot;
    private final int startIndex;   // inclusive
    private final int endIndex;     // exclusive
    private final long totalSystemMemoryBytes;
    private final int logicalProcessorCount;
    private final Map<Integer, OSProcess> priorSnapshot;

    /**
     * Executes the divide-and-conquer scan recursively until leaf threshold is reached.
     *
     * @return map of PID -> ProcessModel collected from this segment and all subtasks
     */
    @Override
    protected ConcurrentHashMap<Integer, ProcessModel> compute() {
        int segmentSize = endIndex - startIndex;

        if (segmentSize <= LEAF_THRESHOLD)
            return processLeaf();

        int midIndex = startIndex + segmentSize / 2;
        ProcessScanTask leftTask = new ProcessScanTask(osProcessSnapshot, startIndex, midIndex, totalSystemMemoryBytes, logicalProcessorCount, priorSnapshot);
        ProcessScanTask rightTask = new ProcessScanTask(osProcessSnapshot, midIndex, endIndex, totalSystemMemoryBytes, logicalProcessorCount, priorSnapshot);

        leftTask.fork();
        ConcurrentHashMap<Integer, ProcessModel> rightResults = rightTask.compute();
        ConcurrentHashMap<Integer, ProcessModel> leftResults = leftTask.join();

        ConcurrentHashMap<Integer, ProcessModel> merged = new ConcurrentHashMap<>(leftResults.size() + rightResults.size());
        merged.putAll(leftResults);
        merged.putAll(rightResults);
        return merged;
    }

    /**
     * Processes the leaf segment, extracting metrics and creating ProcessModel objects.
     * <p>
     * Gracefully handles: null/blank names (skipped), process termination mid-scan (skipped),
     * and access denied errors (included with 0.0 metrics).
     * </p>
     *
     * @return map of PID -> ProcessModel for this leaf segment
     */
    private ConcurrentHashMap<Integer, ProcessModel> processLeaf() {
        ConcurrentHashMap<Integer, ProcessModel> results = new ConcurrentHashMap<>();

        for (int i = startIndex; i < endIndex; i++) {
            OSProcess proc = osProcessSnapshot.get(i);
            ProcessMetrics metrics = extractMetrics(proc);

            if (metrics != null) {
                results.put(metrics.getPid(), new ProcessModel(
                        metrics.getName(), Category.UNCATEGORIZED, 0L,
                        metrics.getRamPercent(), metrics.getCpuPercent(), 0, 0,
                        metrics.getPid(), metrics.getStartTime()));
            }
        }

        return results;
    }

    /**
     * Extracts process metrics safely, handling various error conditions.
     *
     * @param proc the OS process to extract metrics from
     * @return ProcessMetrics if successfully extracted, null if process should be skipped
     */
    private ProcessMetrics extractMetrics(OSProcess proc) {
        try {
            String processName = proc.getName();
            if (processName == null || processName.isBlank()) {
                return null;
            }

            long residentSetSizeBytes = Math.max(0, proc.getResidentSetSize());
            double ramPercent = totalSystemMemoryBytes > 0 ? residentSetSizeBytes * 100.0 / totalSystemMemoryBytes : 0.0;
            int pid = proc.getProcessID();
            if (pid == 0) return null; // System Idle Process — kernel artifact, not a real process

            long startTime = proc.getStartTime();

            OSProcess prior = priorSnapshot.get(pid);
            double cpuPercent = prior != null
                    ? Math.min(100.0, proc.getProcessCpuLoadBetweenTicks(prior) * 100.0 / Math.max(1, logicalProcessorCount))
                    : 0.0;

            return new ProcessMetrics(processName, ramPercent, cpuPercent, pid, startTime);

        } catch (NullPointerException | NoSuchElementException e) {
            return null; // process terminated between snapshot and metric read
        } catch (Exception e) {
            // access denied or other OS restriction — include with 0.0 metrics
            try {
                String processName = proc.getName();
                if (processName == null || processName.isBlank()) return null;
                return new ProcessMetrics(processName, 0.0, 0.0, 0, 0L);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
