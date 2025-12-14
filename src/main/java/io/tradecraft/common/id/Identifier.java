package io.tradecraft.common.id;

import java.util.UUID;

public sealed interface Identifier permits ChildClOrdId, ChildId, ClOrdId, ExecId, ParentId, IntentId, SorChildOrderId, SorEventId, SorOrderId, VenueOrderId {
    // Factory method

    static String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    static String join(String prefix, String core) {
        if (prefix == null || prefix.isBlank())
            return core;
        return prefix.endsWith("-") ? prefix + core : prefix + "-" + core;
    }
    String value();
}