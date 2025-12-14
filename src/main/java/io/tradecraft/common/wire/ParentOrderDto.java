package io.tradecraft.common.wire;

import java.util.List;

public record ParentOrderDto(
        String parentOrderId,   // stable internal id (null for NEW)
        String clOrdId,         // FIX 11
        String account,         // FIX 1
        String symbol,          // FIX 55
        byte side,            // 1=Buy, 2=Sell (FIX 54)
        long qty,             // integer units (FIX 38)
        byte ordType,         // 1=Market,2=Limit,3=Stop,4=StopLimit... (FIX 40)
        Long limitPxMicros,   // price in micro-units; 0 if not applicable
        byte tif,             // 0=Day,1=GTC,3=IOC,4=FOK (FIX 59)
        String exDest,          // optional (FIX 100)
        String strategy,        // e.g. POV/TWAP (free-form)
        List<TagKeyValue> tags,       // deterministic list, not Map
        long createdTsNanos,  // epoch nanos
        int schemaVersion    // bump when evolving fields
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
}


