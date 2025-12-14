package io.tradecraft.common.domain.time;

// Production: system time
public class SystemTimeSource implements TimeSource {
    @Override
    public long nowNanos() {
        return System.nanoTime();
    }
}



