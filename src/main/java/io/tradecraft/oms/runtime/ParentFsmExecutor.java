package io.tradecraft.oms.runtime;

import io.tradecraft.common.envelope.Meta;
import io.tradecraft.oms.core.Effects;
import io.tradecraft.oms.event.OrderEvent;

public interface ParentFsmExecutor {
    Effects apply(OrderEvent event, Meta meta);
}
