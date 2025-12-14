package io.tradecraft.venue.event;

import io.tradecraft.common.id.*;

public record VenueReplaceAck(
        VenueId venueId,
        VenueOrderId venueOrderId,
        ChildClOrdId childClOrdId,
        Long newPriceMicros,
        Long newLeavesQty,
        long tsNanos
) implements VenueEvent {

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private VenueId venueId;
        private VenueOrderId venueOrderId;
        private ChildClOrdId childClOrdId;
        private Long newPriceMicros;
        private Long newLeavesQty;
        private long tsNanos;

        public Builder venueId(VenueId v) { this.venueId = v; return this; }
        public Builder venueOrderId(VenueOrderId v) { this.venueOrderId = v; return this; }
        public Builder childClOrdId(ChildClOrdId v) { this.childClOrdId = v; return this; }
        public Builder newPriceMicros(Long v) { this.newPriceMicros = v; return this; }
        public Builder newLeavesQty(Long v) { this.newLeavesQty = v; return this; }
        public Builder tsNanos(long v) { this.tsNanos = v; return this; }

        public VenueReplaceAck build() {
            return new VenueReplaceAck(venueId, venueOrderId, childClOrdId, newPriceMicros, newLeavesQty, tsNanos);
        }
    }
}
