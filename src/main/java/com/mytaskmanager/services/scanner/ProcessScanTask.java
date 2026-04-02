package com.mytaskmanager.services.scanner;

import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessMetrics;
import com.mytaskmanager.domain.ProcessModel;
import lombok.RequiredArgsConstructor;
import oshi.software.os.OSProcess;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
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
public class ProcessScanTask extends RecursiveTask<List<ProcessModel>> {

    private static final int LEAF_THRESHOLD = 10;

    private final List<OSProcess> osProcessSnapshot;
    private final int startIndex;   // inclusive
    private final int endIndex;     // exclusive
    private final long totalSystemMemoryBytes;
    private final int logicalProcessorCount;

    /**
     * Executes the divide-and-conquer scan recursively until leaf threshold is reached.
     *
     * @return list of ProcessModel objects collected from this segment and all subtasks
     */
    @Override
    protected List<ProcessModel> compute() {
        int segmentSize = endIndex - startIndex;

        if (segmentSize <= LEAF_THRESHOLD)
            return processLeaf();


        int midIndex = startIndex + segmentSize / 2;
        ProcessScanTask leftTask = new ProcessScanTask(osProcessSnapshot, startIndex, midIndex, totalSystemMemoryBytes, logicalProcessorCount);
        ProcessScanTask rightTask = new ProcessScanTask(osProcessSnapshot, midIndex, endIndex, totalSystemMemoryBytes, logicalProcessorCount);

        leftTask.fork();
        List<ProcessModel> rightResults = rightTask.compute();
        List<ProcessModel> leftResults = leftTask.join();

        List<ProcessModel> merged = new ArrayList<>(leftResults.size() + rightResults.size());
        merged.addAll(leftResults);
        merged.addAll(rightResults);
        return merged;
    }

    /**
     * Processes the leaf segment, extracting metrics and creating ProcessModel objects.
     * <p>
     * Gracefully handles: null/blank names (skipped), process termination mid-scan (skipped),
     * and access denied errors (included with 0.0 metrics).
     * </p>
     *
     * @return list of ProcessModel objects for this leaf segment
     */
    private List<ProcessModel> processLeaf() {
        List<ProcessModel> results = new ArrayList<>(endIndex - startIndex);

        for (int i = startIndex; i < endIndex; i++) {
            OSProcess proc = osProcessSnapshot.get(i);
            ProcessMetrics metrics = extractMetrics(proc);

            if (metrics != null)
                results.add(new ProcessModel(metrics.getName(), Category.UNCATEGORIZED, 0L, metrics.getRamPercent(), metrics.getCpuPercent(), 0,  0   ));

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
            double ramPercent = totalSystemMemoryBytes > 0 ? residentSetSizeBytes * 100.0 / totalSystemMemoryBytes: 0.0;
            double cpuPercent = proc.getProcessCpuLoadCumulative() * 100.0/ Math.max(1, logicalProcessorCount); // Mateja ispravi ovo sranje

            return new ProcessMetrics(processName, ramPercent, cpuPercent);

        } catch (NullPointerException | NoSuchElementException e) {
            return null; // process terminated between snapshot and metric read
        } catch (Exception e) {
            // access denied or other OS restriction — include with 0.0 metrics
            try {
                String processName = proc.getName();
                if (processName == null || processName.isBlank()) return null;
                return new ProcessMetrics(processName, 0.0, 0.0);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
