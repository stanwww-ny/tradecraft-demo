package io.tradecraft.common.domain.time;

// Testing: stepping time
public class SteppingTimeSource implements TimeSource {
    private final long step;
    private long current;

    public SteppingTimeSource(long start, long step) {
        this.current = start;
        this.step = step;
    }

    @Override
    public long nowNanos() {
        long t = current;
        current += step;
        return t;
    }
}