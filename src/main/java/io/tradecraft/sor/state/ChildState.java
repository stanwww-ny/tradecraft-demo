package io.tradecraft.sor.state;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.domain.order.CancelReason;
import io.tradecraft.common.domain.order.RejectReason;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

import java.time.Instant;
import java.util.Objects;

// ───────────────────────────────────────────────────────────────────────────────
// ChildState: source of truth for a single child order
public final class ChildState {
    public final ParentId parentId;
    public final ChildId childId;
    public final ChildClOrdId childClOrdId;

    // from intent/cmd/ack:
    public final InstrumentKey instrumentKey;
    public final VenueId venueId;          // set on ack
    public final VenueOrderId venueOrderId;     // set on ack
    public final DomainTif tif;              // inherited from parent; confirmed on ack
    public final DomainOrdType ordType;          // optional; fill if you track it
    public final Instant expireAt;         // nullable (only for GTT)
    public final DomainSide side;
    public final long qty;
    // running fill stats:
    public final long cumQty;           // cumulative filled qty
    public final long leavesQty;
    public final Long lastPxMicros;     // last fill price
    public final ExecId lastExecId;       // last exec id (nullable)

    public final ChildStatus status;
    public final CancelReason cancelReason;
    public final RejectReason rejectReason;
    public final long createdTsNanos;   // when first seen (ACK)
    public final long updatedTsNanos;   // last state change

    private ChildState(Builder b) {
        this.parentId = b.parentId;
        this.childId = b.childId;
        this.childClOrdId = b.childClOrdId;
        this.instrumentKey = b.instrumentKey;
        this.venueId = b.venueId;
        this.venueOrderId = b.venueOrderId;

        this.side = b.side;
        this.qty = b.qty;
        this.tif = b.tif;
        this.ordType = b.ordType;
        this.expireAt = b.expireAt;

        this.cumQty = b.cumQty;
        this.leavesQty = b.leavesQty;
        this.lastPxMicros = b.lastPxMicros;
        this.lastExecId = b.lastExecId;
        this.status = b.status;
        this.cancelReason = b.cancelReason;

        this.rejectReason = b.rejectReason;
        this.createdTsNanos = b.createdTsNanos;
        this.updatedTsNanos = b.updatedTsNanos;
    }

    public static Builder builder(ParentId pid, ChildId cid, long tsNanos) {
        return new Builder(pid, cid, tsNanos);
    }

    public ParentId parentId() {
        return parentId;
    }
    public ChildId childId() {
        return childId;
    }
    public ChildClOrdId childClOrdId() {
        return childClOrdId;
    }
    public InstrumentKey instrumentKey() { return instrumentKey; }
    public VenueId venueId() {
        return venueId;
    }
    public VenueOrderId venueOrderId() {
        return venueOrderId;
    }

    public DomainSide side() { return side; }
    public long qty() { return qty; }
    public DomainTif tif() { return tif; }
    public DomainOrdType ordType() { return ordType; }
    public Instant expireAt() { return expireAt; }

    public long cumQty() { return cumQty; }
    public long leavesQty() { return leavesQty; }
    public long lastPxMicros() { return lastPxMicros; }
    public ExecId lastExecId() { return lastExecId; }
    public ChildStatus status() { return status; }
    public CancelReason cancelReason() { return cancelReason; }

    public RejectReason rejectReason() { return rejectReason; }
    public long createdTsNanos() { return createdTsNanos; }
    public long updatedTsNanos() { return updatedTsNanos; }


    public Builder toBuilder() {
        return new Builder(this);
    }

    public boolean isFinal() {
        return status == ChildStatus.FILLED
                || status == ChildStatus.CANCELED
                || status == ChildStatus.REJECTED
                || status == ChildStatus.EXPIRED;
    }

    public static final class Builder {
        private final ParentId parentId;
        private final ChildId childId;
        private ChildClOrdId childClOrdId;
        private InstrumentKey instrumentKey;
        private VenueId venueId;
        private VenueOrderId venueOrderId;

        private DomainSide side;
        private long qty;
        private DomainTif tif;
        private DomainOrdType ordType;
        private Instant expireAt;

        private long cumQty;
        private long leavesQty;
        private Long lastPxMicros;
        private ExecId lastExecId;
        private ChildStatus status;
        private CancelReason cancelReason;
        private RejectReason rejectReason;

        private final long createdTsNanos;
        private long updatedTsNanos;

        private Builder(ParentId pid, ChildId cid, long tsNanos) {
            this.parentId = Objects.requireNonNull(pid);
            this.childId = Objects.requireNonNull(cid);
            this.status = ChildStatus.NEW_PENDING;
            this.createdTsNanos = tsNanos;
            this.updatedTsNanos = tsNanos;
        }

        private Builder(ChildState s) {
            this.parentId = s.parentId;
            this.childId = s.childId;
            this.childClOrdId = s.childClOrdId;
            this.instrumentKey = s.instrumentKey;
            this.venueId = s.venueId;
            this.venueOrderId = s.venueOrderId;

            this.side = s.side;
            this.qty = s.qty;
            this.tif = s.tif;
            this.ordType = s.ordType;
            this.expireAt = s.expireAt;

            this.cumQty = s.cumQty;
            this.lastPxMicros = s.lastPxMicros;
            this.lastExecId = s.lastExecId;
            this.status = s.status;
            this.cancelReason = s.cancelReason;

            this.rejectReason = s.rejectReason;
            this.createdTsNanos = s.createdTsNanos;
            this.updatedTsNanos = s.updatedTsNanos;
        }

        public Builder childClOrdId(ChildClOrdId childClOrdId) {
            this.childClOrdId = childClOrdId;
            return this;
        }

        public Builder instrumentKey(InstrumentKey instrumentKey) {
            this.instrumentKey = instrumentKey;
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

        public Builder side(DomainSide side) {
            this.side = side;
            return this;
        }

        public Builder qty(long qty) {
            this.qty = qty;
            return this;
        }

        public Builder tif(DomainTif tif) {
            this.tif = tif;
            return this;
        }

        public Builder ordType(DomainOrdType ordType) {
            this.ordType = ordType;
            return this;
        }

        public Builder expireAt(Instant t) {
            this.expireAt = t;
            return this;
        }

        public Builder cumQty(long q) {
            this.cumQty = q;
            return this;
        }

        public Builder leavesQty(long leavesQty) {
            this.leavesQty = leavesQty;
            return this;
        }

        public Builder lastPxMicros(Long px) {
            this.lastPxMicros = px;
            return this;
        }

        public Builder lastExecId(ExecId id) {
            this.lastExecId = id;
            return this;
        }

        public Builder status(ChildStatus st) {
            this.status = st;
            return this;
        }

        public Builder cancelReason(CancelReason cr) {
            this.cancelReason = cr;
            return this;
        }

        public Builder rejectReason(RejectReason r) {
            this.rejectReason = r;
            return this;
        }

        public Builder updatedTsNanos(long t) {
            this.updatedTsNanos = t;
            return this;
        }

        public ChildState build() {
            return new ChildState(this);
        }
    }
}
