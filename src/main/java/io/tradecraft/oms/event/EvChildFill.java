package io.tradecraft.oms.event;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

public record EvChildFill(
        ParentId parentId,
        long tsNanos,
        ChildId childId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        ExecId execId,
        long lastQty,
        long lastPxMicros,
        long cumQty,
        long leavesQty,
        boolean isFinal
) implements OrderEvent {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private long tsNanos;
        private ChildId childId;
        private VenueId venueId;
        private VenueOrderId venueOrderId;
        private ExecId execId;
        private long lastQty;
        private long lastPxMicros;
        private long cumQty;
        private long leavesQty;
        private boolean isFinal;


        public Builder parentId(ParentId v) { this.parentId = v; return this; }
        public Builder childId(ChildId v) { this.childId = v; return this; }
        public Builder venueId(VenueId v) { this.venueId = v; return this; }
        public Builder venueOrderId(VenueOrderId v) { this.venueOrderId = v; return this; }
        public Builder execId(ExecId v) { this.execId = v; return this; }
        public Builder tsNanos(long v) { this.tsNanos = v; return this; }

        public Builder lastQty(long v) { this.lastQty = v; return this; }
        public Builder lastPxMicros(long v) { this.lastPxMicros = v; return this; }
        public Builder cumQty(long v) { this.cumQty = v; return this; }
        public Builder leavesQty(long v) { this.leavesQty = v; return this; }
        public Builder isFinal(boolean v) { this.isFinal = v; return this; }

        public EvChildFill build() {
            return new EvChildFill(parentId, tsNanos, childId, venueId, venueOrderId, execId, lastQty,
                    lastPxMicros, cumQty, leavesQty, isFinal);
        }
    }
}
