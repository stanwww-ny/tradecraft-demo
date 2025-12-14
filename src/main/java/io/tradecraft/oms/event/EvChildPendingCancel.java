package io.tradecraft.oms.event;

import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

/**
 * Emitted when a child order has entered a pending-cancel state.
 * Typically after sending CancelChildCmd to the venue but before
 * receiving a final CancelDone or CancelReject.
 */
public record EvChildPendingCancel(
        ParentId parentId,
        ChildId childId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        ChildClOrdId childClOrdId,
        long tsNanos
) implements OrderEvent {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private ChildId childId;
        private VenueId venueId;
        private VenueOrderId venueOrderId;
        private ChildClOrdId childClOrdId;
        private long tsNanos;

        public Builder parentId(ParentId parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder childId(ChildId childId) {
            this.childId = childId;
            return this;
        }

        public Builder venueId(VenueId venueId) {
            this.venueId = venueId;
            return this;
        }

        public Builder venueOrderId(VenueOrderId venueOrderId) {
            this.venueOrderId = venueOrderId;
            return this;
        }

        public Builder childClOrdId(ChildClOrdId childClOrdId) {
            this.childClOrdId = childClOrdId;
            return this;
        }

        public Builder tsNanos(long tsNanos) {
            this.tsNanos = tsNanos;
            return this;
        }

        public EvChildPendingCancel build() {
            return new EvChildPendingCancel(
                    parentId,
                    childId,
                    venueId,
                    venueOrderId,
                    childClOrdId,
                    tsNanos
            );
        }
    }
}
