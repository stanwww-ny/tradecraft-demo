package io.tradecraft.oms.core;

import io.tradecraft.common.id.ParentId;

public interface ParentStateStore {
    OrderState get(ParentId id);

    void put(OrderState state);
}