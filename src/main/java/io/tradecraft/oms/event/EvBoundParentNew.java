package io.tradecraft.oms.event;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.fixqfj.session.SessionKey;

public record EvBoundParentNew(
        ParentId parentId,
        long tsNanos,
        SessionKey sessionKey,
        ClOrdId clOrdId,
        String accountId,
        DomainAccountType domainAccountType,
        InstrumentKey instrumentKey,
        DomainSide side,
        long qty,              // parent quantity
        DomainOrdType ordType,
        Long limitPxMicros,
        DomainTif tif,
        String exDest
) implements OrderEvent {
}