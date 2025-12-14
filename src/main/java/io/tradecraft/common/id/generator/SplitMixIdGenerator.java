package io.tradecraft.common.id.generator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Fast, thread-safe, deterministic generator producing short hex IDs with a prefix. Good for prod and tests
 * (seedable).
 */
public final class SplitMixIdGenerator implements IdGenerator<String> {
    private final String prefix;
    private final AtomicLong counter;
    private final long seedSalt;

    public SplitMixIdGenerator(String prefix, long startAt, long seedSalt) {
        this.prefix = prefix;
        this.counter = new AtomicLong(startAt);
        this.seedSalt = seedSalt;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    @Override
    public String next() {
        long v = counter.incrementAndGet();
        long x = mix64(v ^ seedSalt); // <<< salt makes streams disjoint across nodes/boots
        return prefix + "-" + Long.toHexString(x);
    }
}

