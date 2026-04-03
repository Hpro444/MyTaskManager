package com.mytaskmanager.services.fileIo;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FileIoScheduler}.
 * <p>
 * Verifies that the single-threaded file I/O executor submits and executes tasks,
 * and that submissions after shutdown are safely ignored without throwing exceptions.
 * </p>
 */
class FileIoSchedulerTest {

    /**
     * Tests that the scheduler executes submitted tasks.
     */
    @Test
    void submit_runsTask() throws InterruptedException {
        FileIoScheduler scheduler = new FileIoScheduler();
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.submit(latch::countDown);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        scheduler.shutdownAndAwait();
    }

    /**
     * Tests that submissions after shutdown are ignored gracefully.
     */
    @Test
    void submit_afterShutdown_doesNotThrowAndDoesNotRunTask() {
        FileIoScheduler scheduler = new FileIoScheduler();
        AtomicInteger counter = new AtomicInteger(0);

        scheduler.shutdownAndAwait();

        assertDoesNotThrow(() -> scheduler.submit(counter::incrementAndGet));
        assertEquals(0, counter.get());
    }
}

