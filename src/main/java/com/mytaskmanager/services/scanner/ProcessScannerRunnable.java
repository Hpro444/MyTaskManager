package com.mytaskmanager.services.scanner;

import com.mytaskmanager.domain.ProcessModel;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Runnable that performs one full process scan and delivers the result
 * to the provided callback. The caller is responsible for dispatching
 * the callback onto the appropriate thread (e.g. Platform.runLater for UI).
 */
@RequiredArgsConstructor
public class ProcessScannerRunnable implements Runnable {

    private final ProcessScannerService scannerService;
    private final Consumer<ConcurrentHashMap<Integer, ProcessModel>> onScanComplete;

    @Override
    public void run() {
        try {
            ConcurrentHashMap<Integer, ProcessModel> result = scannerService.scanProcesses();
            onScanComplete.accept(result);
        } catch (Exception e) {
            // Prevent ScheduledExecutorService from silently cancelling all future scans
            e.printStackTrace();
        }
    }
}
