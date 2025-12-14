package io.tradecraft.venue.model;

import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;
import io.tradecraft.venue.cmd.NewChildCmd;

/**
 * Internal representation of an order resting in the fake venueId book. Immutable style with copy/update helpers.
 *
 * @param childId       from OMS/SOR
 * @param venueOrderId  synthetic, unique inside venueId
 * @param side          BUY or SELL
 * @param ordType       LIMIT or MARKET
 * @param limitPxMicros valid only if LIMIT
 * @param entryTsNanos  time inserted to book
 */
public record VenueOrder(ParentId parentId, ChildId childId, ChildClOrdId childClOrdId, VenueId venueId,
                         VenueOrderId venueOrderId, DomainSide side, long qty, DomainOrdType ordType,
                         Long limitPxMicros, long cumQty, long leavesQty, long entryTsNanos) {
    // constructor private — use factory

    /**
     * Factory from inbound NewChildCmd
     */
    public static VenueOrder from(NewChildCmd cmd, VenueId venueId, VenueOrderId venueOrderId, long tsNanos) {
        return new VenueOrder(
                cmd.parentId(),
                cmd.childId(),
                cmd.childClOrdId(),
                venueId,
                venueOrderId,
                cmd.side(),
                cmd.qty(),
                cmd.ordType(),
                cmd.priceMicros(),
                0L,
                cmd.qty(),
                tsNanos
        );
    }

    public boolean isMarket() {
        return ordType == DomainOrdType.MARKET;
    }

    // --- update helpers ---
    public VenueOrder withCum(long newCum) {
        return new VenueOrder(parentId, childId, childClOrdId, venueId, venueOrderId, side, qty, ordType,
                limitPxMicros, newCum, leavesQty, entryTsNanos);
    }

    public VenueOrder withLeaves(long newLeaves) {
        return new VenueOrder(parentId, childId, childClOrdId, venueId, venueOrderId, side, qty, ordType,
                limitPxMicros, cumQty, newLeaves, entryTsNanos);
    }

    public VenueOrder withCumAndLeaves(long newCum, long newLeaves) {
        return new VenueOrder(parentId, childId, childClOrdId, venueId, venueOrderId, side, qty, ordType,
                limitPxMicros, newCum, newLeaves, entryTsNanos);
    }

}