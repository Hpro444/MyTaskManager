package com.mytaskmanager.services.scanner;

import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ProcessScannerServiceTest {

    private static ConcurrentHashMap<Integer, ProcessModel> scannedProcesses;

    @BeforeAll
    static void runScan() {
        ProcessScannerService scanner = new ProcessScannerService();
        scannedProcesses = scanner.scanProcesses();
    }

    @Test
    void scanProcesses_returnsNonNullList() {
        assertNotNull(scannedProcesses);
    }

    @Test
    void scanProcesses_listIsNotEmpty() {
        assertFalse(scannedProcesses.isEmpty(), "Expected at least one running process on the system");
    }

    @Test
    void scanProcesses_allProcessNamesAreNonBlank() {
        for (ProcessModel process : scannedProcesses.values()) {
            assertNotNull(process.getName(), "Process name must not be null");
            assertFalse(process.getName().isBlank(), "Process name must not be blank");
        }
    }

    @Test
    void scanProcesses_ramRanksAreAssigned() {
        for (ProcessModel process : scannedProcesses.values()) {
            assertTrue(process.getRamRank() >= 1,
                    "Expected ramRank >= 1 for process: " + process.getName());
        }
    }

    @Test
    void scanProcesses_cpuRanksAreAssigned() {
        for (ProcessModel process : scannedProcesses.values()) {
            assertTrue(process.getCpuRank() >= 1,
                    "Expected cpuRank >= 1 for process: " + process.getName());
        }
    }

    @Test
    void scanProcesses_topRamRankIsOne() {
        ProcessModel highestRamProcess = scannedProcesses.values().stream()
                .max(Comparator.comparingDouble(ProcessModel::getRamUsagePercent))
                .orElseThrow();
        assertEquals(1, highestRamProcess.getRamRank(),
                "The process with the highest RAM usage must have ramRank == 1");
    }

    @Test
    void scanProcesses_topCpuRankIsOne() {
        ProcessModel highestCpuProcess = scannedProcesses.values().stream()
                .max(Comparator.comparingDouble(ProcessModel::getCpuUsagePercent))
                .orElseThrow();
        assertEquals(1, highestCpuProcess.getCpuRank(),
                "The process with the highest CPU usage must have cpuRank == 1");
    }

    @Test
    void scanProcesses_canBeCalledMultipleTimes() {
        ProcessScannerService scanner = new ProcessScannerService();
        ConcurrentHashMap<Integer, ProcessModel> firstScan = scanner.scanProcesses();
        ConcurrentHashMap<Integer, ProcessModel> secondScan = scanner.scanProcesses();
        assertNotNull(firstScan);
        assertNotNull(secondScan);
    }

    @Test
    void scanProcesses_allCategoriesAreUncategorized() {
        for (ProcessModel process : scannedProcesses.values()) {
            assertEquals(Category.UNCATEGORIZED, process.getCategory(),
                    "Fresh scan must default to UNCATEGORIZED for: " + process.getName());
        }
    }

    @Test
    void scanProcesses_allTotalSecondsAreZero() {
        for (ProcessModel process : scannedProcesses.values()) {
            assertEquals(0L, process.getTotalSeconds(),
                    "Fresh scan must have totalSeconds == 0 for: " + process.getName());
        }
    }
}
