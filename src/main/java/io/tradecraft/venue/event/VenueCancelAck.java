package io.tradecraft.venue.event;

import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

/**
 * Emitted when the venueId confirms successful cancellation. Equivalent to FIX 35=9 (ExecType=4).
 */
public record VenueCancelAck(
        ChildId childId,
        ChildClOrdId childClOrdId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        long tsNanos
) implements VenueEvent {

    public static Builder builder() {
        return new Builder();
    }

    @Override

    public ChildClOrdId childClOrdId() { return childClOrdId; }
    public VenueId venueId() { return venueId; }
    public VenueOrderId venueOrderId() { return venueOrderId; }

    public static final class Builder {
        private ChildId childId;
        private ChildClOrdId childClOrdId;
        private VenueId venueId;
        private VenueOrderId venueOrderId;
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

        public Builder tsNanos(long tsNanos) {
            this.tsNanos = tsNanos;
            return this;
        }

        public VenueCancelAck build() {
            return new VenueCancelAck(childId, childClOrdId, venueId, venueOrderId, tsNanos);
        }
    }
}

