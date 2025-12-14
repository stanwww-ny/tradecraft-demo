package io.tradecraft.oms.core;

import io.tradecraft.common.id.IntentId;
import io.tradecraft.oms.event.EvNew;

public class DefaultNewStateMapper implements NewStateMapper {
    public OrderState from(EvNew e, IntentId intentId) {
        return new OrderState(
                e.parentId(),
                e.clOrdId(),
                e.instrumentKey(),
                e.side(),
                e.tif(),
                e.tif().computeExpireAt(e.tsNanos(), null),
                e.qty(),
                e.tsNanos(),
                intentId
        );

    }
}
