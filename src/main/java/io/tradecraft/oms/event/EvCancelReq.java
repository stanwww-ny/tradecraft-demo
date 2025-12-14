package io.tradecraft.oms.event;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.ParentId;

public record EvCancelReq(
        ParentId parentId,
        long tsNanos,
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
