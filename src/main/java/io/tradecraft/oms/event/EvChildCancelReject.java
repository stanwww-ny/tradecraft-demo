package io.tradecraft.oms.event;

import io.tradecraft.common.domain.order.CancelRejectReason;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

/**
 * Emitted when a child order cancel request is rejected by the venue.
 * Typically transitions from PENDING_CANCEL → REJECTED or back to ACKED
 * depending on venue semantics.
 */
public record EvChildCancelReject(
        ParentId parentId,
        ChildId childId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        ChildClOrdId childClOrdId,
        CancelRejectReason reason,
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
        private CancelRejectReason reason;
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

        public Builder reason(CancelRejectReason reason) {
            this.reason = reason;
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

        public EvChildCancelReject build() {
            return new EvChildCancelReject(
                    parentId,
                    childId,
                    venueId,
                    venueOrderId,
                    childClOrdId,
                    reason,
                    text,
                    tsNanos
            );
        }
    }
}

