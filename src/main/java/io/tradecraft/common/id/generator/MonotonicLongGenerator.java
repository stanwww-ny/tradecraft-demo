package io.tradecraft.common.id.generator;

import java.util.concurrent.atomic.AtomicLong;

public final class MonotonicLongGenerator implements IdGenerator<Long> {
    private final AtomicLong counter;

    public MonotonicLongGenerator(long startAt) {
        this.counter = new AtomicLong(startAt);
    }

    @Override
    public Long next() {
        return counter.incrementAndGet();
    }

    @Override
    public String toString() {
        return "MonotonicLongGenerator[start=" + counter.get() + "]";
    }
}