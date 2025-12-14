package io.tradecraft.oms.event;

import io.tradecraft.common.domain.order.RejectReason;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

/**
 * Emitted when a child order replace (amend) request is rejected by the venue.
 * Typically follows a VenueReplaceReject and indicates that the child
 * remains in its previous ACKED or PARTIALLY_FILLED state.
 */
public record EvChildReplaceReject(
        ParentId parentId,
        ChildId childId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        ChildClOrdId childClOrdId,
        RejectReason rejectReason,
        String text,
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
        private RejectReason rejectReason;
        private String text;
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

        public Builder replaceRejectReason(RejectReason reason) {
            this.rejectReason = reason;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder tsNanos(long tsNanos) {
            this.tsNanos = tsNanos;
            return this;
        }

        public EvChildReplaceReject build() {
            return new EvChildReplaceReject(
                    parentId,
                    childId,
                    venueId,
                    venueOrderId,
                    childClOrdId,
                    rejectReason,
                    text,
                    tsNanos
            );
        }
    }
}
