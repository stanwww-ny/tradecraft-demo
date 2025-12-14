package io.tradecraft.oms.runtime;

import io.tradecraft.common.id.ParentId;
import io.tradecraft.oms.repo.ClOrdIndex;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory map of (SessionKey, ClOrdId) → ParentOrderId. Designed to run inside the single-writer pipeline thread.
 */
public final class DefaultClOrdIndex implements ClOrdIndex {

    private final Map<SessionClOrdKey, ParentId> map = new HashMap<>();

    @Override
    public ParentId get(SessionClOrdKey k) {
        return map.get(k);
    }

    @Override
    public ParentId putIfAbsent(SessionClOrdKey k, ParentId pid) {
        return map.putIfAbsent(k, pid);
    }

    /**
     * For testing / monitoring
     */
    public int size() {
        return map.size();
    }

    public Iterable<Map.Entry<SessionClOrdKey, ParentId>> entries() {
        return map.entrySet();
    }
}

