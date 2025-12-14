package io.tradecraft.oms.event;

import io.tradecraft.common.domain.order.RejectReason;
import io.tradecraft.common.domain.order.Source;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

public record EvChildReject(
        ParentId parentId,
        long tsNanos,
        ChildId childId,
        ChildClOrdId childClOrdId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        Source source,     // typically "VENUE" or "SOR"
        String text,
        RejectReason rejectReason
) implements OrderEvent {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private long tsNanos;
        private ChildId childId;
        ChildClOrdId childClOrdId;
        VenueId venueId;
        VenueOrderId venueOrderId;
        Source source;
        String text;
        private RejectReason rejectReason;

        public Builder parentId(ParentId v) { this.parentId = v; return this; }
        public Builder childId(ChildId v) { this.childId = v; return this; }
        public Builder childClOrdId(ChildClOrdId v) { this.childClOrdId = v; return this; }
        public Builder venueId(VenueId v) { this.venueId = v; return this; }
        public Builder venueOrderId(VenueOrderId v) { this.venueOrderId = v; return this; }
        public Builder source(Source v) { this.source = v; return this;}
        public Builder rejectReason(RejectReason v) { this.rejectReason = v; return this; }
        public Builder text(String v) { this.text = v; return this;}
        public Builder tsNanos(long v) { this.tsNanos = v; return this; }

        public EvChildReject build() {
            return new EvChildReject(parentId, tsNanos, childId,
                    childClOrdId, venueId, venueOrderId, source, text, rejectReason);
        }
    }
}
