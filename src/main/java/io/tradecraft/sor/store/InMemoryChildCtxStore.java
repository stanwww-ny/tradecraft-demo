package io.tradecraft.sor.store;

import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.spi.sor.intent.NewChildIntent;
import io.tradecraft.sor.state.ParentRouteCtx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryChildCtxStore implements ChildCtxStore {
    private final Map<ParentId, ParentRouteCtx> parents = new ConcurrentHashMap<>();

    public ParentRouteCtx get(ParentId parentId) {
        return parents.get(parentId);
    }

    public ParentRouteCtx computeIfAbsent(NewChildIntent i) {
        return parents.computeIfAbsent(i.parentId(), id ->
                new ParentRouteCtx(id, i.accountId(), i.accountType(), i.instrumentKey(), i.tif()));
    }

    @Override
    public ParentRouteCtx put(NewChildIntent i) {
        return parents.put(i.parentId(), new ParentRouteCtx(i.parentId(), i.accountId(), i.accountType(), i.instrumentKey(), i.tif()));
    }

    @Override
    public ParentRouteCtx remove(ParentId parentId) {
        return parents.remove(parentId);
    }


}
