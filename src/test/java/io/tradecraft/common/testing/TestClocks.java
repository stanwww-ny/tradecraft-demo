package io.tradecraft.common.testing;

import io.tradecraft.common.domain.time.DualTimeSource;

public final class TestClocks {
    public static final long DEFAULT_WALL_MS = 1_700_000_000_000L;

    private TestClocks() {
    }

    public static DualTimeSource frozen() {
        return DualTimeSource.test(1L, 0L, DEFAULT_WALL_MS);
    }

    public static DualTimeSource microTicker() {
        return DualTimeSource.test(1L, 1000L, DEFAULT_WALL_MS);
    }

    public static DualTimeSource msTicker() {
        return DualTimeSource.test(1L, 1000_000L, DEFAULT_WALL_MS);
    }

}

