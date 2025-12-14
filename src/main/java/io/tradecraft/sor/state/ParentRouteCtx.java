package io.tradecraft.sor.state;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ParentId;

public record ParentRouteCtx(
        ParentId parentId,
        String accountId,
        DomainAccountType accountType,
        InstrumentKey instrument,
        DomainTif parentTif
) {
}