package com.mytaskmanager.services.scanner;

import com.mytaskmanager.domain.Category;
import com.mytaskmanager.domain.ProcessModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProcessScannerService}.
 * <p>
 * Verifies that process scanning works correctly, including rank assignment,
 * process name validation, and repeated scan behavior. Tests run against live
 * system processes to ensure the integration with OSHI library functions properly.
 * </p>
 */
class ProcessScannerServiceTest {

    private static ConcurrentHashMap<Integer, ProcessModel> scannedProcesses;

    /**
     * Performs an initial process scan before all tests.
     */
    @BeforeAll
    static void runScan() {
        ProcessScannerService scanner = new ProcessScannerService();
        scannedProcesses = scanner.scanProcesses();
    }

    /**
     * Tests that scanProcesses returns a non-null result.
     */
    @Test
    void scanProcesses_returnsNonNullList() {
        assertNotNull(scannedProcesses);
    }

    /**
     * Tests that at least one process is found on the system.
     */
    @Test
    void scanProcesses_listIsNotEmpty() {
        assertFalse(scannedProcesses.isEmpty(), "Expected at least one running process on the system");
    }

    /**
     * Tests that all process names are non-blank.
     */
    @Test
    void scanProcesses_allProcessNamesAreNonBlank() {
        for (ProcessModel process : scannedProcesses.values()) {
            assertNotNull(process.getName(), "Process name must not be null");
            assertFalse(process.getName().isBlank(), "Process name must not be blank");
        }
    }

    /**
     * Tests that RAM ranks are assigned to all processes.
     */
    @Test
    void scanProcesses_ramRanksAreAssigned() {
        for (ProcessModel process : scannedProcesses.values()) {
            assertTrue(process.getRamRank() >= 1,
                    "Expected ramRank >= 1 for process: " + process.getName());
        }
    }

    /**
     * Tests that CPU ranks are assigned to all processes.
     */
    @Test
    void scanProcesses_cpuRanksAreAssigned() {
        for (ProcessModel process : scannedProcesses.values()) {
            assertTrue(process.getCpuRank() >= 1,
                    "Expected cpuRank >= 1 for process: " + process.getName());
        }
    }

    /**
     * Tests that the process with highest RAM usage has rank 1.
     */
    @Test
    void scanProcesses_topRamRankIsOne() {
        ProcessModel highestRamProcess = scannedProcesses.values().stream()
                .max(Comparator.comparingDouble(ProcessModel::getRamUsagePercent))
                .orElseThrow();
        assertEquals(1, highestRamProcess.getRamRank(),
                "The process with the highest RAM usage must have ramRank == 1");
    }

    /**
     * Tests that the process with highest CPU usage has rank 1.
     */
    @Test
    void scanProcesses_topCpuRankIsOne() {
        ProcessModel highestCpuProcess = scannedProcesses.values().stream()
                .max(Comparator.comparingDouble(ProcessModel::getCpuUsagePercent))
                .orElseThrow();
        assertEquals(1, highestCpuProcess.getCpuRank(),
                "The process with the highest CPU usage must have cpuRank == 1");
    }

    /**
     * Tests that the scanner can be called multiple times on the same instance.
     */
    @Test
    void scanProcesses_canBeCalledMultipleTimes() {
        ProcessScannerService scanner = new ProcessScannerService();
        ConcurrentHashMap<Integer, ProcessModel> firstScan = scanner.scanProcesses();
        ConcurrentHashMap<Integer, ProcessModel> secondScan = scanner.scanProcesses();
        assertNotNull(firstScan);
        assertNotNull(secondScan);
    }

    /**
     * Tests that all processes default to UNCATEGORIZED category.
     */
    @Test
    void scanProcesses_allCategoriesAreUncategorized() {
        for (ProcessModel process : scannedProcesses.values()) {
            assertEquals(Category.UNCATEGORIZED, process.getCategory(),
                    "Fresh scan must default to UNCATEGORIZED for: " + process.getName());
        }
    }

    /**
     * Tests that all processes start with zero total seconds.
     */
    @Test
    void scanProcesses_allTotalSecondsAreZero() {
        for (ProcessModel process : scannedProcesses.values()) {
            assertEquals(0L, process.getTotalSeconds(),
                    "Fresh scan must have totalSeconds == 0 for: " + process.getName());
        }
    }
}
