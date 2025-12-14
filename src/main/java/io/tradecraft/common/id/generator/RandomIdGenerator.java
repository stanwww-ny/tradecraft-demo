package io.tradecraft.common.id.generator;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Non-deterministic ID generator using ThreadLocalRandom. Useful for production when reproducibility isn't needed.
 */
public final class RandomIdGenerator implements IdGenerator<String> {

    private final String prefix;

    public RandomIdGenerator(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String next() {
        long x = ThreadLocalRandom.current().nextLong();
        return prefix + "-" + Long.toHexString(x);
    }

    @Override
    public String toString() {
        return "RandomIdGenerator[prefix=" + prefix + "]";
    }
}

