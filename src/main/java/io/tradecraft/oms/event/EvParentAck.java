package io.tradecraft.oms.event;

import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueOrderId;

import java.time.Instant;

public record EvParentAck(
        ParentId parentId,
        long tsNanos,
        ChildId childId,
        VenueOrderId venueOrderId,
        DomainTif domainTif,
        Instant expireAt
) implements OrderEvent {
}
