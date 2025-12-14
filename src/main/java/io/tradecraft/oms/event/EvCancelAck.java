package io.tradecraft.oms.event;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;

public record EvCancelAck(
        ParentId parentId,
        long tsNanos,
        ChildId childId,
        String text
) implements OrderEvent {
}
