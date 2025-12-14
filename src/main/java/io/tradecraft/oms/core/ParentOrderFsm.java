package io.tradecraft.oms.core;

import io.tradecraft.oms.event.OrderEvent;

public interface ParentOrderFsm {
    Effects apply(OrderState stateOrNull, OrderEvent ev);
}