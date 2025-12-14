package io.tradecraft.oms.runtime;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ExecId;

import java.util.concurrent.ConcurrentHashMap;

public class ChildFillDeduper {
    private final ConcurrentHashMap<ChildId, ExecIdWindow> windowByChild = new ConcurrentHashMap<>();
    private final int windowSize;

    public ChildFillDeduper(int windowSize) {
        this.windowSize = windowSize;
    }

    /**
     * @return true if this ExecId is a duplicate and should be ignored.
     */
    public boolean isDuplicate(ChildId childId, ExecId execId) {
        ExecIdWindow win = windowByChild.computeIfAbsent(
                childId, id -> new ExecIdWindow(windowSize));

        boolean isNew = win.markIfNew(execId);
        return !isNew; // duplicate = true
    }
}
