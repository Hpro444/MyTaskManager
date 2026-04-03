package com.mytaskmanager.services.fileIo;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FileIoSchedulerTest {

    @Test
    void submit_runsTask() throws InterruptedException {
        FileIoScheduler scheduler = new FileIoScheduler();
        CountDownLatch latch = new CountDownLatch(1);

        scheduler.submit(latch::countDown);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        scheduler.shutdownAndAwait();
    }

    @Test
    void submit_afterShutdown_doesNotThrowAndDoesNotRunTask() {
        FileIoScheduler scheduler = new FileIoScheduler();
        AtomicInteger counter = new AtomicInteger(0);

        scheduler.shutdownAndAwait();

        assertDoesNotThrow(() -> scheduler.submit(counter::incrementAndGet));
        assertEquals(0, counter.get());
    }
}

