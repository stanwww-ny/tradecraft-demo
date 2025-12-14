package io.tradecraft.fixqfj.event;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.fixqfj.session.SessionKey;

public record FixEvCancelReq(
        SessionKey sessionKey,
        ClOrdId clOrdId,
        ClOrdId origClOrdId,
        String accountId,
        DomainAccountType domainAccountType,
        InstrumentKey instrumentKey,
        DomainSide side,
        Long qty,                       // 38; null = full cancel; >0 = reduce/target
        long ingressNanos
) implements FixEvInbound {
}