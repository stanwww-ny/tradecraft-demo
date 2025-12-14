package io.tradecraft.venue.cmd;

import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;

/**
 * Replace (Cancel/Replace) a child order in-place. Any nullable field that is null means "no change".
 * <p>
 * Typical uses: - quantity decrease (and sometimes increase if allowed by venueId rules) - price update for limit
 * orders - TIF or other flags could be added later if your MVP-1.1 needs it
 */
public record ReplaceChildCmd(
        ParentId parentId,
        ChildId childId,
        ChildClOrdId childClOrdId,   // new client order id for the replace
        Long newQty,                 // null = keep existing
        Long newLimitPxMicros,       // null = keep existing
        VenueId venueId,
        long tsNanos
) implements VenueCommand {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private ChildId childId;
        private ChildClOrdId childClOrdId;
        private long newQty;
        private Long newLimitPxMicros;
        private VenueId venueId;
        private long tsNanos;

        public Builder() {
        }

        public Builder parentId(ParentId parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder childId(ChildId childId) {
            this.childId = childId;
            return this;
        }

        public Builder childClOrdId(ChildClOrdId childClOrdId) {
            this.childClOrdId = childClOrdId;
            return this;
        }

        public Builder newQty(long qty) {
            this.newQty = qty;
            return this;
        }

        public Builder newLimitPxMicros(Long priceMicros) {
            this.newLimitPxMicros = priceMicros;
            return this;
        }


        public Builder venueId(VenueId venueId) {
            this.venueId = venueId;
            return this;
        }

        public Builder tsNanos(long tsNanos) {
            this.tsNanos = tsNanos;
            return this;
        }

        public ReplaceChildCmd build() {
            return new ReplaceChildCmd(
                    parentId,
                    childId,
                    childClOrdId,
                    newQty,
                    newLimitPxMicros,
                    venueId,
                    tsNanos
            );
        }
    }
}
