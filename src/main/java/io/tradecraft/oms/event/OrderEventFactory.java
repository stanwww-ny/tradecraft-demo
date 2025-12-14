package io.tradecraft.oms.event;

import io.tradecraft.common.id.ParentId;

/**
 * Factory helpers to build OMS events with consistent validation and timestamps. Primarily used in tests.
 */
public final class OrderEventFactory {
    public OrderEventFactory() {
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // New Orders
    // ─────────────────────────────────────────────────────────────────────────────

    public EvNew toEvNew(EvBoundParentNew e, ParentId pid) {
        return new EvNew(
                pid,
                e.tsNanos(),
                e.clOrdId(),
                e.accountId(),
                e.domainAccountType(),
                e.instrumentKey(),
                e.side(),
                e.qty(),
                e.ordType(), e.limitPxMicros() == null ? 0 : e.limitPxMicros(),
                e.tif(),
                e.exDest());
    }
}
