package io.tradecraft.oms.event;

import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

import java.time.Instant;

public record EvChildAck(ParentId parentId, ChildId childId, ChildClOrdId childClOrdId,
                         VenueId venueId, VenueOrderId venueOrderId, ExecId execId,
                         DomainTif tif, Instant expireAt,
                         long tsNanos) implements OrderEvent {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private ChildId childId;
        private ChildClOrdId childClOrdId;

        private VenueId venueId;
        private VenueOrderId venueOrderId;
        private ExecId execId;
        private DomainTif tif;
        private Instant expireAt;
        private long tsNanos;

        public Builder parentId(ParentId v) { this.parentId = v; return this; }
        public Builder childId(ChildId v) { this.childId = v; return this; }
        public Builder childClOrdId(ChildClOrdId v) { this.childClOrdId = v; return this; }
        public Builder venueId(VenueId v) { this.venueId = v; return this; }
        public Builder venueOrderId(VenueOrderId v) { this.venueOrderId = v; return this; }
        public Builder execId(ExecId v) { this.execId = v; return this; }
        public Builder tif(DomainTif v) { this.tif = v; return this; }
        public Builder expireAt(Instant v) { this.expireAt = v; return this; }
        public Builder tsNanos(long v) { this.tsNanos = v; return this; }

        public EvChildAck build() {
            return new EvChildAck(parentId, childId, childClOrdId, venueId, venueOrderId, execId, tif, expireAt, tsNanos);
        }
    }
}

