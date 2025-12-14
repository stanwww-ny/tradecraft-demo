package io.tradecraft.venue.event;

import io.tradecraft.common.domain.order.RejectReason;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

public record VenueNewReject(
        VenueId venueId,
        VenueOrderId venueOrderId,   // may be null if rejected pre-orderId
        ChildClOrdId childClOrdId,
        RejectReason reason,
        String text,
        long tsNanos
) implements VenueEvent {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private VenueId venueId;
        private VenueOrderId venueOrderId;
        private ChildClOrdId childClOrdId;
        private RejectReason reason = RejectReason.OTHER;
        private String text = "";
        private long tsNanos;

        public Builder venueId(VenueId v) { this.venueId = v; return this; }
        public Builder venueOrderId(VenueOrderId v) { this.venueOrderId = v; return this; }
        public Builder childClOrdId(ChildClOrdId v) { this.childClOrdId = v; return this; }
        public Builder reason(RejectReason v) { this.reason = v; return this; }
        public Builder text(String v) { this.text = v; return this; }
        public Builder tsNanos(long v) { this.tsNanos = v; return this; }

        public VenueNewReject build() {
            return new VenueNewReject(venueId, venueOrderId, childClOrdId, reason, text, tsNanos);
        }
    }
}
