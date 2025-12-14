package io.tradecraft.oms.runtime;

import io.tradecraft.common.id.ExecId;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded, single-writer cache of recently seen ExecIDs (LRU). Thread-safety: NOT thread-safe by design—use on a single
 * pipeline thread.
 */
public final class ExecIdWindow {
    private final int maxSize;
    private final LinkedHashMap<ExecId, Boolean> lru;

    public ExecIdWindow(int maxSize) {
        this.maxSize = Math.max(64, maxSize); // safety floor
        this.lru = new LinkedHashMap<>(maxSize, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<ExecId, Boolean> eldest) {
                return size() > ExecIdWindow.this.maxSize;
            }
        };
    }

    /**
     * @return true if execId is NEW; false if duplicate (already seen).
     */
    public boolean markIfNew(ExecId execId) {
        if (execId == null) return true; // cannot dedup → treat as new
        return lru.putIfAbsent(execId, Boolean.TRUE) == null;
    }

    /**
     * Remove all tracked execIds.
     */
    public void clear() {
        lru.clear();
    }

    /**
     * Current number of tracked execIds (for testing/metrics).
     */
    public int size() {
        return lru.size();
    }
}
