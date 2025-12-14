package io.tradecraft.oms.runtime;

import io.tradecraft.common.id.ParentId;
import io.tradecraft.oms.core.OrderState;
import io.tradecraft.oms.core.ParentStateStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryParentStateStore implements ParentStateStore {
    private final Map<ParentId, OrderState> map = new ConcurrentHashMap<>();

    @Override
    public OrderState get(ParentId id) {
        return map.get(id);
    }

    @Override
    public void put(OrderState state) {
        if (state == null) {
            throw new IllegalStateException("StateStore.put(null) – caller produced null state");
        }
        map.put(state.parentId(), state);
    }
}
