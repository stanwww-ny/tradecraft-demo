package io.tradecraft.sor.store;

import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.spi.sor.intent.NewChildIntent;
import io.tradecraft.sor.state.ParentRouteCtx;

public interface ChildCtxStore {
    ParentRouteCtx get(ParentId parentOrderId);

    ParentRouteCtx put(NewChildIntent i);

    ParentRouteCtx computeIfAbsent(NewChildIntent i);

    ParentRouteCtx remove(ParentId parentOrderId);
}
