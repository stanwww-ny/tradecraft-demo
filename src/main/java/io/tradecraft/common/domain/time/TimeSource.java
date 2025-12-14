package io.tradecraft.common.domain.time;

import java.util.concurrent.TimeUnit;

public interface TimeSource {
    static TimeSource system() {
        return System::nanoTime;
    }

    long nowNanos();

    default long nowMillis() {
        return TimeUnit.NANOSECONDS.toMillis(nowNanos());
    }
}
