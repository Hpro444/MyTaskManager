package com.mytaskmanager.services;

import lombok.RequiredArgsConstructor;

/**
 * Wraps a single file I/O task and prevents uncaught exceptions from
 * silently terminating the file-io thread in the executor.
 */
@RequiredArgsConstructor
public class FileIoRunnable implements Runnable {

    private final Runnable task;

    @Override
    public void run() {
        try {
            task.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
