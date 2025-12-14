package io.tradecraft.oms.repo;

import io.tradecraft.common.id.ParentId;
import io.tradecraft.oms.runtime.SessionClOrdKey;

// Durable index (ChronicleMap/DB or in-mem for MVP)
public interface ClOrdIndex {
    ParentId get(SessionClOrdKey k);

    ParentId putIfAbsent(SessionClOrdKey k, ParentId pid);
}
