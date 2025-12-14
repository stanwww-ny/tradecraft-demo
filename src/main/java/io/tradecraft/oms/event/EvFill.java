package io.tradecraft.oms.event;

import io.tradecraft.common.id.*;

public record EvFill(
        ParentId parentId,
        long tsNanos,
        ChildId childId,
        ChildClOrdId childClOrdId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        ExecId execId,
        long lastQty,
        Long lastPxMicros,
        long cumQty,
        long leaveQty
) implements OrderEvent {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private long tsNanos;
        private ChildId childId;
        private ChildClOrdId childClOrdId;
        private VenueId venueId;
        private VenueOrderId venueOrderId;
        private ExecId execId;
        private long lastQty;
        private Long lastPxMicros;
        private long cumQty;
        private long leaveQty;

        private Builder() {
        }

        public Builder parentId(ParentId parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder tsNanos(long tsNanos) {
            this.tsNanos = tsNanos;
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

        public Builder lastQty(long lastQty) {
            this.lastQty = lastQty;
            return this;
        }

        public Builder lastPxMicros(Long lastPxMicros) {
            this.lastPxMicros = lastPxMicros;
            return this;
        }

        public Builder cumQty(long cumQty) {
            this.cumQty = cumQty;
            return this;
        }

        public Builder leaveQty(long leaveQty) {
            this.leaveQty = leaveQty;
            return this;
        }

        public EvFill build() {
            return new EvFill(
                    parentId,
                    tsNanos,
                    childId,
                    childClOrdId,
                    venueId,
                    venueOrderId,
                    execId,
                    lastQty,
                    lastPxMicros,
                    cumQty,
                    leaveQty
            );
        }
    }
}
