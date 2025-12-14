package io.tradecraft.common.utils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.*;

class IdleStrategyTest {

    @Test
    void testResetClearsIdleCounter() {
        IdleStrategy idle = new IdleStrategy(3, 6, 1_000_000);

        // idle a few times
        idle.idle();
        idle.idle();
        idle.idle();

        // now reset
        idle.reset();

        // next idle should go back to SPIN phase
        long start = System.nanoTime();
        idle.idle();  // should spin, not yield or park
        long duration = System.nanoTime() - start;

        assertTrue(duration < 1_000_000, "Expected a fast spin after reset");
    }

    @Test
    void testSpinToYieldTransition() {
        IdleStrategy idle = new IdleStrategy(3, 6, 1_000_000);

        AtomicInteger yields = new AtomicInteger();

        Thread t = new Thread(() -> {
            // 1–3: spin
            idle.idle();
            idle.idle();
            idle.idle();

            // 4–6: yield expected
            for (int i = 0; i < 3; i++) {
                long before = System.nanoTime();
                idle.idle();
                long after = System.nanoTime();

                // Yield often increases latency noticeably
                if (after - before > 50_000) { // > 50 microseconds
                    yields.incrementAndGet();
                }
            }
        });

        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            fail("Thread interrupted");
        }

        assertTrue(yields.get() > 0, "Expected some yield-phase behavior");
    }

    @Test
    void testParkPhaseOccurs() {
        long parkNanos = 200_000; // 0.2 ms
        IdleStrategy idle = new IdleStrategy(1, 2, parkNanos);

        // Advance into park phase
        idle.idle(); // 1: spin
        idle.idle(); // 2: yield
        idle.idle(); // 3: should park

        long before = System.nanoTime();
        idle.idle(); // should parkNanos again
        long duration = System.nanoTime() - before;

        assertTrue(
                duration >= parkNanos * 0.8,
                "Expected parkNanos to block at least ~80% of the requested time"
        );
    }

    @Test
    void testNoProgressKeepsIncreasingIdlePhase() {
        IdleStrategy idle = new IdleStrategy(2, 4, 50_000);

        // Just call idle repeatedly without reset
        for (int i = 0; i < 10; i++) {
            idle.idle();
        }

        // Should have reached the park phase by now
        long before = System.nanoTime();
        idle.idle();
        long duration = System.nanoTime() - before;

        assertTrue(duration >= 40_000, "Expected idle() to be in park phase");
    }

    @Test
    void testProgressResetsToSpin() {
        IdleStrategy idle = new IdleStrategy(2, 4, 50_000);

        // Enter park phase
        for (int i = 0; i < 10; i++) idle.idle();

        // Now simulate progress
        idle.reset();

        // Expect spin-phase latency
        long before = System.nanoTime();
        idle.idle();
        long duration = System.nanoTime() - before;

        assertTrue(duration < 1_000_000, "After reset, a fast spin is expected");
    }
}

