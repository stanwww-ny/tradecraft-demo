package io.tradecraft.common.utils;

import java.util.concurrent.locks.LockSupport;

/**
 * A 3-phase idle strategy:
 *  1. Hot spin via Thread.onSpinWait() for extremely short waits
 *  2. Yield to OS scheduler for medium waits
 *  3. parkNanos() for longer idle periods
 *
 *  Typical use in an event loop:
 *
 *      if (!progressed) {
 *          idle.idle();
 *      } else {
 *          idle.reset();
 *      }
 */
public final class IdleStrategy {

    private final int spinTries;
    private final int yieldTries;
    private final long parkNanos;

    private int idleCounter = 0;

    public IdleStrategy(int spinTries, int yieldTries, long parkNanos) {
        this.spinTries = spinTries;
        this.yieldTries = yieldTries;
        this.parkNanos = parkNanos;
    }

    /**
     * Default low-latency settings: - ~50 spins - ~100 yields - parkNanos = 50µs
     */
    public static IdleStrategy defaultStrategy() {
        return new IdleStrategy(50, 150, 50_000);
    }

    /**
     * Apply the idle strategy based on how long we've been idle.
     */
    public void idle() {
        idleCounter++;

        if (idleCounter < spinTries) {
            // Hot spin — stays in user space, almost zero latency
            Thread.onSpinWait();
        } else if (idleCounter < yieldTries) {
            // Let the scheduler run another thread
            Thread.yield();
        } else {
            // Last-level fallback — lightweight park
            LockSupport.parkNanos(parkNanos);
        }
    }

    /**
     * Reset counter when the event loop made progress.
     */
    public void reset() {
        idleCounter = 0;
    }
}