package io.tradecraft.oms.event;

import io.tradecraft.common.id.*;

/**
 * Emitted when a child order replace (amend) has been acknowledged by the venue.
 * This indicates the order was successfully modified (e.g. new price or quantity).
 */
public record EvChildReplaced(
        ParentId parentId,
        ChildId childId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        ChildClOrdId childClOrdId,
        Long newPriceMicros,   // may be null if not changed
        Long newLeavesQty,     // may be null if not changed
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
        private Long newPriceMicros;
        private Long newLeavesQty;
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

        public Builder newPriceMicros(Long newPriceMicros) {
            this.newPriceMicros = newPriceMicros;
            return this;
        }

        public Builder newLeavesQty(Long newLeavesQty) {
            this.newLeavesQty = newLeavesQty;
            return this;
        }

        public Builder tsNanos(long tsNanos) {
            this.tsNanos = tsNanos;
            return this;
        }

        public EvChildReplaced build() {
            return new EvChildReplaced(
                    parentId,
                    childId,
                    venueId,
                    venueOrderId,
                    childClOrdId,
                    newPriceMicros,
                    newLeavesQty,
                    tsNanos
            );
        }
    }
}

