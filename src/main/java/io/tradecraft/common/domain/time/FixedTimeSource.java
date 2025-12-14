package io.tradecraft.common.domain.time;

// Testing: fixed time
public class FixedTimeSource implements TimeSource {
    private final long fixedNanos;

    public FixedTimeSource(long fixedNanos) {
        this.fixedNanos = fixedNanos;
    }

    @Override
    public long nowNanos() {
        return fixedNanos;
    }
}