package io.tradecraft.oms.event;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.fixqfj.session.SessionKey;

public record EvBoundCancelReq(
        ParentId parentId,
        long tsNanos,
        SessionKey sessionKey,
        ClOrdId clOrdId,
        ClOrdId origClOrdId,
        String accountId,
        DomainAccountType domainAccountType,
        InstrumentKey instrumentKey,
        DomainSide side,
        Long qty,
        String exDest,
        String reason
) implements OrderEvent {
}
