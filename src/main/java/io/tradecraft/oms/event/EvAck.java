package io.tradecraft.oms.event;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

public record EvAck(
        ParentId parentId,
        ChildId childId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        ExecId execId,
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
        private ExecId execId;
        private long tsNanos;

        private Builder() {
        }

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

        public Builder execId(ExecId execId) {
            this.execId = execId;
            return this;
        }

        public Builder tsNanos(long tsNanos) {
            this.tsNanos = tsNanos;
            return this;
        }

        public EvAck build() {
            return new EvAck(parentId, childId, venueId, venueOrderId, execId, tsNanos);
        }
    }
}