package io.tradecraft.oms.runtime;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ParentCancelRegistry {
    private final ConcurrentMap<ParentId, java.util.Set<ChildId>> byParent = new ConcurrentHashMap<>();

    public void mark(ParentId pid) {
        byParent.putIfAbsent(pid, ConcurrentHashMap.newKeySet());
    }

    public boolean isMarked(ParentId pid) {
        return byParent.containsKey(pid);
    }

    /** Returns true exactly once per (parent, child). */
    public boolean markChildIfFirst(ParentId pid, ChildId cid) {
        return byParent.computeIfAbsent(pid, k -> ConcurrentHashMap.newKeySet()).add(cid);
    }

    public void clear(ParentId pid) {
        byParent.remove(pid);
    }
}

