// org/example/v4/oms/fsm/event/EvError.java
package io.tradecraft.oms.event;

import io.tradecraft.common.id.ParentId;

public record EvError(
        ParentId parentId,
        long tsNanos,
        String message
) implements OrderEvent {
}
