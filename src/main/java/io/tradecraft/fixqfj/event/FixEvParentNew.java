package io.tradecraft.fixqfj.event;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.fixqfj.session.SessionKey;

public record FixEvParentNew(
        // [A] IDs & context
        SessionKey sessionKey,
        ClOrdId clOrdId,
        String accountId,
        DomainAccountType domainAccountType,

        // [B] Instrument
        InstrumentKey instrumentKey,

        // [C] Side/Qty
        DomainSide side, // 1=Buy, 2=Sell (FIX 5
        long qty, // FIX 38

        // [D] Price/Type
        DomainOrdType ordType, // FIX 40
        Long limitPxMicros,// nullable for non-limit

        // [E] TIF
        DomainTif tif, // FIX 59

        // [F] Routing
        String exDest, // FIX 100 optional

        // [G] Timing
        long ingressNanos
) implements FixEvInbound {
}