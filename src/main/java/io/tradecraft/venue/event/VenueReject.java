package io.tradecraft.venue.event;

import io.tradecraft.common.domain.order.RejectReason;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

/**
 * VenueReject – venueId rejects a new child order.
 */
public record VenueReject(
        ChildId childId,
        ChildClOrdId childClOrdId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        RejectReason reason,
        String text,
        long tsNanos
) implements VenueEvent {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ChildId childId;
        private ChildClOrdId childClOrdId;
        private VenueId venueId;
        private VenueOrderId venueOrderId;
        private RejectReason reason;
        private String text;
        private long tsNanos;

        public Builder childId(ChildId childId) {
            this.childId = childId;
            return this;
        }

        public Builder childClOrdId(ChildClOrdId childClOrdId) {
            this.childClOrdId = childClOrdId;
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

        public Builder rejectReason(RejectReason reason) {
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

        public VenueReject build() {
            return new VenueReject(childId, childClOrdId, venueId, venueOrderId, reason, text, tsNanos);
        }
    }
}


