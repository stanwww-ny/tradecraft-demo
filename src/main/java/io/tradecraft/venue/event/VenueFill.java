package io.tradecraft.venue.event;

import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;
import io.tradecraft.venue.model.VenueOrder;

public record VenueFill(
        ParentId parentId,
        ChildId childId,
        ChildClOrdId childClOrdId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        ExecId execId,
        long lastQty,
        long lastPxMicros,
        long cumQty,
        boolean isFinal,
        long tsNanos
) implements VenueEvent {

    public static VenueFill of(VenueOrder vo, ExecId execId, long lastQty, long lastPxMicros, long cumQty, boolean isFinal, long tsNanos) {
        return new VenueFill(
                vo.parentId(),
                vo.childId(),
                vo.childClOrdId(),
                vo.venueId(),
                vo.venueOrderId(),
                execId,
                lastQty,
                lastPxMicros,      // <- use the computed price, NOT cmd.priceMicros()
                cumQty,
                isFinal,
                tsNanos
        );
    }
}
