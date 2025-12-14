package io.tradecraft.venue.event;

import io.tradecraft.common.domain.order.CancelReason;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

import java.util.Objects;

public record VenueCancelDone(
        ChildId childId,
        ChildClOrdId childClOrdId,
        VenueId venueId,
        VenueOrderId venueOrderId,
        ExecId execId,
        CancelReason reason,
        Long canceledQty,       // nullable = unknown/not provided
        long tsNanos
) implements VenueEvent {

    // Compact canonical ctor for validation
    public VenueCancelDone {
        Objects.requireNonNull(childId, "childId");
        Objects.requireNonNull(venueId, "venueId");
        Objects.requireNonNull(venueOrderId, "venueOrderId");
        Objects.requireNonNull(execId, "execId");
        Objects.requireNonNull(reason, "rejectReason");
        if (tsNanos < 0) throw new IllegalArgumentException("tsNanos < 0");
        if (canceledQty != null && canceledQty < 0) {
            throw new IllegalArgumentException("canceledQty < 0");
        }
    }

    // Legacy factory (no canceledQty) for existing call sites
    public static VenueCancelDone of(ChildId childId,
                                     ChildClOrdId childClOrdId,
                                     VenueId venueId,
                                     VenueOrderId venueOrderId,
                                     ExecId execId,
                                     CancelReason reason,
                                     Long canceledQty,
                                     long tsNanos) {
        return new VenueCancelDone(childId, childClOrdId, venueId, venueOrderId, execId, reason, canceledQty, tsNanos);
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ChildId childId;
        private ChildClOrdId childClOrdId;
        private VenueId venueId;
        private VenueOrderId venueOrderId;
        private ExecId execId;
        private CancelReason reason;
        private Long canceledQty;    // keep nullable to allow "unspecified"
        private long tsNanos;

        private Builder() {
        }

        public Builder childId(ChildId v) {
            this.childId = v;
            return this;
        }

        public Builder childClOrdId(ChildClOrdId v) {
            this.childClOrdId = v;
            return this;
        }

        public Builder venueId(VenueId v) {
            this.venueId = v;
            return this;
        }

        public Builder venueOrderId(VenueOrderId v) {
            this.venueOrderId = v;
            return this;
        }

        public Builder execId(ExecId v) {
            this.execId = v;
            return this;
        }

        public Builder reason(CancelReason v) {
            this.reason = v;
            return this;
        }

        public Builder tsNanos(long v) {
            this.tsNanos = v;
            return this;
        }

        public Builder canceledQty(long v) {
            this.canceledQty = v;
            return this;
        } // convenience

        public VenueCancelDone build() {
            return new VenueCancelDone(childId, childClOrdId, venueId, venueOrderId, execId, reason, canceledQty, tsNanos);
        }
    }
}
