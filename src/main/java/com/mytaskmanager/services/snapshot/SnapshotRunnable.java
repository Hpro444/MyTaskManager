package com.mytaskmanager.services.snapshot;

import com.mytaskmanager.domain.ProcessSnapshot;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

/**
 * Runnable that performs one snapshot check tick.
 * Uses a {@link Supplier} for the process list to stay decoupled from the analytics package.
 */
@RequiredArgsConstructor
public class SnapshotRunnable implements Runnable {

    private final Supplier<List<ProcessSnapshot>> snapshotSupplier;
    private final SnapshotService snapshotService;

    @Override
    public void run() {
        try {
            snapshotService.checkAndTrigger(snapshotSupplier.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
