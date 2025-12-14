package io.tradecraft.common.domain.time;

import java.util.Iterator;
import java.util.List;

public class ReplayTimeSource implements TimeSource {
    private final Iterator<Long> recordedTimes;
    private long lastTime;

    public ReplayTimeSource(List<Long> recordedTimes) {
        this.recordedTimes = recordedTimes.iterator();
    }

    @Override
    public long nowNanos() {
        if (recordedTimes.hasNext()) {
            lastTime = recordedTimes.next();
        }
        return lastTime;  // stays pinned until next event
    }
}