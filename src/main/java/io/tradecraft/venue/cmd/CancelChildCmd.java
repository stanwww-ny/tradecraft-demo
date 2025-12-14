package io.tradecraft.venue.cmd;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

public record CancelChildCmd(
        ParentId parentId,
        ChildId childId,
        ChildClOrdId childClOrdId,
        InstrumentKey instrumentKey,
        VenueId venueId,
        VenueOrderId venueOrderId,
        long tsNanos
) implements VenueCommand {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private ChildId childId;
        private ChildClOrdId childClOrdId;
        private InstrumentKey instrumentKey;
        private VenueId venueId;
        private VenueOrderId venueOrderId;
        private long tsNanos;

        public Builder() {
        }

        public CancelChildCmd.Builder parentId(ParentId parentId) {
            this.parentId = parentId;
            return this;
        }

        public CancelChildCmd.Builder childId(ChildId childId) {
            this.childId = childId;
            return this;
        }

        public CancelChildCmd.Builder childClOrdId(ChildClOrdId childClOrdId) {
            this.childClOrdId = childClOrdId;
            return this;
        }

        public CancelChildCmd.Builder instrumentKey(InstrumentKey instrumentKey) {
            this.instrumentKey = instrumentKey;
            return this;
        }

        public CancelChildCmd.Builder venueId(VenueId venueId) {
            this.venueId = venueId;
            return this;
        }

        public CancelChildCmd.Builder venueOrderId(VenueOrderId venueOrderId) {
            this.venueOrderId = venueOrderId;
            return this;
        }

        public CancelChildCmd.Builder tsNanos(long tsNanos) {
            this.tsNanos = tsNanos;
            return this;
        }

        public CancelChildCmd build() {
            return new CancelChildCmd(
                    parentId,
                    childId,
                    childClOrdId,
                    instrumentKey,
                    venueId,
                    venueOrderId,
                    tsNanos
            );
        }
    }
}
