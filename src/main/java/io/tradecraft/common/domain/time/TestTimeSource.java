package io.tradecraft.common.domain.time;

import java.util.concurrent.atomic.AtomicLong;

public final class TestTimeSource implements TimeSource {
    private final AtomicLong nanos;
    private final long step;

    public TestTimeSource(long startNanos, long step) {
        this.nanos = new AtomicLong(startNanos);
        this.step = step;
    }

    public static TestTimeSource startAt(long startNanos) {
        return new TestTimeSource(startNanos, 1L);
    }

    public static TestTimeSource startAt(long startNanos, long stepNanos) {
        return new TestTimeSource(startNanos, stepNanos);
    }

    @Override
    public long nowNanos() {
        return nanos.getAndAdd(step);
    }

    public long advance(long delta) {
        return nanos.getAndAdd(delta);
    }

    public void set(long value) {
        nanos.set(value);
    }
}
