package io.tradecraft.venue.event;

import io.tradecraft.common.domain.order.CancelRejectReason;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

/**
 * Emitted when the venueId rejects a cancel request. Equivalent to FIX 35=9 (CxlRejReason > 0).
 */
public record VenueCancelReject(
        ChildId childId,
        ChildClOrdId childClOrdId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        CancelRejectReason reason,
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
        private CancelRejectReason reason = CancelRejectReason.OTHER;
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

        public VenueCancelReject build() {
            return new VenueCancelReject(childId, childClOrdId, venueId, venueOrderId, reason, text, tsNanos);
        }
    }
}

