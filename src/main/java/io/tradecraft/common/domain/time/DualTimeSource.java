package io.tradecraft.common.domain.time;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Dual time provider: - nowNanos(): monotonic, for sequencing/latency (like System.nanoTime()) - wallClockMillis(): UTC
 * milliseconds since epoch, for logging/compliance NEVER mix the two for ordering
 * <p>
 * How to use it ============= (1) In order-producer / venueId strategy (prod) final DualTimeSource ts =
 * DualTimeSource.system(); // ... long timeNanos = ts.nowNanos();         // for price–time priority long
 * transactTimeMs = ts.wallClockMillis(); // for FIX TransactTime/logs book.addResting(vo, childId, side, pxMicros, qty,
 * timeNanos);
 * <p>
 * (2) In JUnit tests DualTimeSource.TestDualTimeSource ts = DualTimeSource.test( 1L, 1L, 1_700_000_000_000L); OrderBook
 * book = new SimpleOrderBook(); ((SimpleOrderBook) book).addResting(vo, childId, DomainSide.SELL, 100_50L, 40L,
 * ts.nowNanos()); ((SimpleOrderBook) book).addResting(vo2, childId2, DomainSide.SELL, 101_00L, 60L, ts.nowNanos()); If
 * your test needs to simulate clock progress for logs: ts.advanceWallClockMillis(5);
 */
public interface DualTimeSource {

    static DualTimeSource system() {
        return SystemDualTimeSource.getInstance();
    }

    static TestDualTimeSource test(long startNanos, long stepNanos, long startMillis) {
        return new TestDualTimeSource(startNanos, stepNanos, startMillis);
    }

    static DualTimeSource compose(LongSupplier monotonicNanos, LongSupplier wallClockMillis) {
        return new ComposedDualTimeSource(monotonicNanos, wallClockMillis);
    }

    /**
     * Monotonic nanoseconds for ordering and measuring durations.
     */
    long nowNanos();

    /**
     * Wall-clock milliseconds since epoch (UTC) for logs / FIX timestamps.
     */
    long wallClockMillis();

    /**
     * Convenience: wall-clock as Instant.
     */
    default Instant wallClockInstant() {
        return Instant.ofEpochMilli(wallClockMillis());
    }

    /**
     * Convenience: wall-clock nanos (derived).
     */
    default long wallClockNanos() {
        return wallClockMillis() * 1_000_000L;
    }

    // -------------------- Implementations --------------------

    /**
     * Convenience: duration since a previous monotonic reading.
     */
    default Duration sinceNanos(long earlierNowNanos) {
        long delta = nowNanos() - earlierNowNanos;
        // Avoid negative duration if a test double misbehaves:
        return Duration.ofNanos(Math.max(0L, delta));
    }

    /**
     * Adapter: expose monotonic as LongSupplier (e.g., Chronicle / metrics).
     */
    default LongSupplier monotonicSupplier() {
        return this::nowNanos;
    }

    /**
     * Adapter: expose wall-clock as LongSupplier.
     */
    default LongSupplier wallClockMillisSupplier() {
        return this::wallClockMillis;
    }

    // -------------------- Factories --------------------

    /**
     * Production: monotonic = System.nanoTime(), wall-clock = System.currentTimeMillis(). Thread-safe and
     * allocation-free.
     */
    final class SystemDualTimeSource implements DualTimeSource {
        private static final SystemDualTimeSource INSTANCE = new SystemDualTimeSource();

        private SystemDualTimeSource() {
        }

        public static SystemDualTimeSource getInstance() {
            return INSTANCE;
        }

        @Override
        public long nowNanos() {
            return System.nanoTime();
        }

        @Override
        public long wallClockMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "SystemDualTimeSource";
        }
    }

    /**
     * Test / Simulation / Replay: - Monotonic nanos: deterministic counter with configurable step. - Wall clock millis:
     * independently controlled epoch millis.
     * <p>
     * By default, only nowNanos() advances the monotonic counter. Wall-clock advances only when you call
     * advanceWallClock(...) or setWallClockMillis(...).
     */
    final class TestDualTimeSource implements DualTimeSource {
        private final AtomicLong monotonicNanos;
        private final long monotonicStepNanos;
        private final AtomicLong wallClockMillis;

        /**
         * Start monotonic at startNanos (inclusive), adding step each nowNanos() call; wall clock at startMillis.
         */
        public TestDualTimeSource(long startNanos, long stepNanos, long startMillis) {
            if (stepNanos <= 0) throw new IllegalArgumentException("stepNanos must be > 0");
            this.monotonicNanos = new AtomicLong(startNanos);
            this.monotonicStepNanos = stepNanos;
            this.wallClockMillis = new AtomicLong(startMillis);
        }

        @Override
        public long nowNanos() {
            return monotonicNanos.getAndAdd(monotonicStepNanos);
        }

        @Override
        public long wallClockMillis() {
            return wallClockMillis.get();
        }

        // ---- Test control APIs ----

        /**
         * Advance monotonic by an arbitrary delta (can be negative for what-if tests, though discouraged).
         */
        public long advanceMonotonicNanos(long delta) {
            return monotonicNanos.getAndAdd(delta);
        }

        /**
         * Force monotonic to a specific value.
         */
        public void setMonotonicNanos(long value) {
            monotonicNanos.set(value);
        }

        /**
         * Advance wall clock forward by deltaMillis (can be negative, but beware logs).
         */
        public long advanceWallClockMillis(long deltaMillis) {
            return wallClockMillis.getAndAdd(deltaMillis);
        }

        /**
         * Set wall clock to a specific epoch millis.
         */
        public void setWallClockMillis(long epochMillis) {
            wallClockMillis.set(epochMillis);
        }

        /**
         * Optionally couple both clocks for scenarios where each event should tick both.
         */
        public void coupleTicks(long wallClockMillisPerTick) {
            // Example usage: wrap nowNanos() to also bump wall-clock (if you want coupling).
            // Left as a no-op by default to keep semantics clean.
        }

        @Override
        public String toString() {
            return "TestDualTimeSource[nanos=" + monotonicNanos.get() + ", step=" + monotonicStepNanos
                    + ", wallMillis=" + wallClockMillis.get() + "]";
        }
    }

    /**
     * Adapter: compose from two separate providers (e.g., legacy code already has both).
     */
    final class ComposedDualTimeSource implements DualTimeSource {
        private final LongSupplier monotonic;
        private final LongSupplier wallMillis;

        public ComposedDualTimeSource(LongSupplier monotonicNanos, LongSupplier wallClockMillis) {
            this.monotonic = Objects.requireNonNull(monotonicNanos);
            this.wallMillis = Objects.requireNonNull(wallClockMillis);
        }

        @Override
        public long nowNanos() {
            return monotonic.getAsLong();
        }

        @Override
        public long wallClockMillis() {
            return wallMillis.getAsLong();
        }

        @Override
        public String toString() {
            return "ComposedDualTimeSource";
        }
    }
}
