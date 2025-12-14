package io.tradecraft.common.spi.oms.exec;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.ExecKind;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;
import io.tradecraft.common.spi.Outgoing;
import io.tradecraft.oms.core.OrderStatus;

/**
 * Public ExecReport to publish out of OMS (to FIX encoder, logs, etc.). DTO-only: fields + small static factories. Keep
 * FIX-tag decisions in the encoder.
 */
public record PubExecReport(
        ParentId parentId,
        ClOrdId clOrdId,
        ClOrdId origClOrdId,
        ChildId childId,      // null for parent-level ERs
        VenueId venueId,
        VenueOrderId venueOrderId,
        ExecId execId,
        InstrumentKey instrumentKey,    // may be null for minimal ERs
        DomainSide domainSide,          // may be null for minimal ERs
        ExecKind execKind,
        OrderStatus status,
        long lastQty,                   // 0 for non-trade ERs
        long cumQty,
        long leavesQty,
        long lastPxMicros,
        long avgPxMicros,               // OK to be 0; FIX layer can omit tag 6 when cum=0
        long tsNanos,
        String reason
) implements Outgoing {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private ClOrdId clOrdId;
        private ClOrdId origClOrdId;
        private ChildId childId;
        private VenueId venueId;
        private VenueOrderId venueOrderId;
        private ExecId execId;
        private InstrumentKey instrumentKey;
        private DomainSide domainSide;
        private ExecKind execKind;
        private OrderStatus status;
        private long lastQty;
        private long cumQty;
        private long leavesQty;
        private long lastPxMicros;
        private long avgPxMicros;
        private long tsNanos;
        private String reason = "";

        public Builder parentId(ParentId v) {
            this.parentId = v;
            return this;
        }

        public Builder clOrdId(ClOrdId v) {
            this.clOrdId = v;
            return this;
        }

        public Builder origClOrdId(ClOrdId v) {
            this.origClOrdId = v;
            return this;
        }

        public Builder childId(ChildId v) {
            this.childId = v;
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

        public Builder instrumentKey(InstrumentKey v) {
            this.instrumentKey = v;
            return this;
        }

        public Builder domainSide(DomainSide v) {
            this.domainSide = v;
            return this;
        }

        public Builder execKind(ExecKind v) {
            this.execKind = v;
            return this;
        }

        public Builder status(OrderStatus v) {
            this.status = v;
            return this;
        }

        public Builder lastQty(long v) {
            this.lastQty = v;
            return this;
        }

        public Builder cumQty(long v) {
            this.cumQty = v;
            return this;
        }

        public Builder leavesQty(long v) {
            this.leavesQty = v;
            return this;
        }

        public Builder lastPxMicros(long v) {
            this.lastPxMicros = v;
            return this;
        }

        public Builder avgPxMicros(long v) {
            this.avgPxMicros = v;
            return this;
        }

        public Builder tsNanos(long v) {
            this.tsNanos = v;
            return this;
        }

        public Builder reason(String v) {
            this.reason = v;
            return this;
        }

        public PubExecReport build() {
            return new PubExecReport(
                    parentId, clOrdId, origClOrdId, childId,
                    venueId, venueOrderId, execId,
                    instrumentKey, domainSide,
                    execKind, status,
                    lastQty, cumQty, leavesQty,
                    lastPxMicros, avgPxMicros,
                    tsNanos, reason
            );
        }
    }
}
