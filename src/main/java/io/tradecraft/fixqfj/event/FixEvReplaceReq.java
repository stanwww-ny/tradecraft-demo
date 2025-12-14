package io.tradecraft.fixqfj.event;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.fixqfj.session.SessionKey;

public record FixEvReplaceReq(
        SessionKey sessionKey,
        ClOrdId clOrdId,
        ClOrdId origClOrdId,
        String accountId,
        DomainAccountType domainAccountType,
        InstrumentKey instrumentKey,
        DomainSide side,
        Long qty,
        DomainOrdType ordType,
        Long limitPxMicros,
        DomainTif tif,
        String exDest,
        long ingressNanos
) implements FixEvInbound {
}
