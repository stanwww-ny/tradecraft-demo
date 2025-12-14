package io.tradecraft.oms.event;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.fixqfj.session.SessionKey;

public record EvBoundReplaceReq(
        // [A] IDs & context
        ParentId parentId,
        long tsNanos,
        SessionKey sessionKey,
        ClOrdId clOrdId,         // new replace ID
        ClOrdId origClOrdId,     // the currently active order ID
        String accountId,
        DomainAccountType domainAccountType,

        // [B] Instrument (usually unchanged)
        InstrumentKey instrumentKey,

        // [C] Side/Qty
        DomainSide side,            // should not change
        Long qty,             // new target qty; null ⇒ unchanged

        // [D] Price/Type
        DomainOrdType ordType,         // null ⇒ unchanged
        Long limitPxMicros,   // null ⇒ unchanged

        // [E] TIF
        DomainTif tif,             // null ⇒ unchanged

        // [F] Routing
        String exDest
) implements OrderEvent {
}
