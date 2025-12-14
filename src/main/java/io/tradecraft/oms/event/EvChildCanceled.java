package io.tradecraft.oms.event;

import io.tradecraft.common.domain.order.CancelReason;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

public record EvChildCanceled(
        ParentId parentId,
        long tsNanos,
        ChildId childId,
        ChildClOrdId childClOrdId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        ExecId execId,
        CancelReason cancelReason
) implements OrderEvent {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private long tsNanos;
        private ChildId childId;
        private ChildClOrdId childClOrdId;
        private VenueId venueId;
        private VenueOrderId venueOrderId;
        private ExecId execId;
        private CancelReason cancelReason;

        public Builder parentId(ParentId v) { this.parentId = v; return this; }
        public Builder childId(ChildId v) { this.childId = v; return this; }
        public Builder childClOrdId(ChildClOrdId v) { this.childClOrdId = v; return this; }
        public Builder venueId(VenueId v) { this.venueId = v; return this; }
        public Builder venueOrderId(VenueOrderId v) { this.venueOrderId = v; return this; }
        public Builder execId(ExecId v) { this.execId = v; return this; }
        public Builder cancelReason(CancelReason v) { this.cancelReason = v; return this; }
        public Builder tsNanos(long v) { this.tsNanos = v; return this; }

        public EvChildCanceled build() {
            return new EvChildCanceled(parentId, tsNanos, childId, childClOrdId,
                    venueId, venueOrderId, execId, cancelReason);
        }
    }
}
