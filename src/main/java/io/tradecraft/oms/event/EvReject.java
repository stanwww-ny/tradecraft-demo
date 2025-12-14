package io.tradecraft.oms.event;

import io.tradecraft.common.id.ParentId;

public record EvReject(
        ParentId parentId,
        long tsNanos,
        String source,         // e.g., "OMS", "SOR", "VENUE"
        String reason
) implements OrderEvent {
}
