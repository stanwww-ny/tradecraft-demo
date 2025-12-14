package io.tradecraft.venue.event;

import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;
import io.tradecraft.venue.model.VenueOrder;

public record VenueAck(
        ParentId parentId,
        ChildId childId,
        ChildClOrdId childClOrdId,
        VenueId venueId,
        VenueOrderId venueOrderId, // optional (some venues assign this)
        ExecId execId,
        long tsNanos
) implements VenueEvent {
    public static VenueAck of(VenueOrder vo, ExecId execId, long tsNanos) {
        return new VenueAck(
                vo.parentId(),
                vo.childId(),
                vo.childClOrdId(),
                vo.venueId(),
                vo.venueOrderId(),
                execId,
                tsNanos
        );
    }

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

        public VenueAck build() {
            return new VenueAck(parentId, childId, childClOrdId, venueId, venueOrderId, execId, tsNanos);
        }
    }
}