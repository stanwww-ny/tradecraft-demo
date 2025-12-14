// src/main/java/io/tradecraft/common/spi/oms/intent/ParentRouteIntent.java
package io.tradecraft.common.spi.oms.intent;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.domain.validation.OrdTypeValidator;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.IntentId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * OMS -> SOR: route a parent order (possibly split into children).
 */
public record ParentRouteIntent(
        ParentId parentId,
        ClOrdId clOrdId,
        String accountId,
        DomainAccountType accountType,
        InstrumentKey instrumentKey,
        DomainSide side,
        long parentQty,
        long leavesQty,
        DomainOrdType ordType,
        Long limitPxMicros,          // null for MARKET
        DomainTif tif,
        Instant expireAt,            // nullable
        String exDest,               // selected venueId (if already chosen)
        List<VenueId> candidateVenues,
        Long targetChildQty,         // nullable: SOR decides
        int maxParallelChildren,
        boolean postOnly,
        boolean iocOnly,
        IntentId intentId,
        int intentRevision,
        long tsNanos                  // keep last for append-friendly evolution
) implements PubParentIntent {

    /* ----------- Builder-in-record ----------- */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private ClOrdId clOrdId;
        private String accountId;
        private DomainAccountType accountType;
        private InstrumentKey instrumentKey;
        private DomainSide side;
        private long parentQty;
        private long leavesQty;
        private DomainOrdType ordType;
        private Long limitPxMicros;
        private DomainTif tif;
        private Instant expireAt;
        private String exDest;
        private List<VenueId> candidateVenues = List.of();
        private Long targetChildQty;
        private int maxParallelChildren = 1;
        private boolean postOnly;
        private boolean iocOnly;
        private IntentId intentId = IntentId.of("INTENT-DEFAULT");
        private int intentRevision;
        private long tsNanos;

        public Builder parentId(ParentId v) {
            this.parentId = v;
            return this;
        }

        public Builder clOrdId(ClOrdId v) {
            this.clOrdId = v;
            return this;
        }

        public Builder accountId(String v) {
            this.accountId = v;
            return this;
        }

        public Builder accountType(DomainAccountType v) {
            this.accountType = v;
            return this;
        }

        public Builder instrumentKey(InstrumentKey v) {
            this.instrumentKey = v;
            return this;
        }

        public Builder side(DomainSide v) {
            this.side = v;
            return this;
        }

        public Builder parentQty(long v) {
            this.parentQty = v;
            return this;
        }

        public Builder leavesQty(long v) {
            this.leavesQty = v;
            return this;
        }

        public Builder ordType(DomainOrdType v) {
            this.ordType = v;
            return this;
        }

        public Builder limitPxMicros(Long v) {
            this.limitPxMicros = v;
            return this;
        }

        public Builder tif(DomainTif v) {
            this.tif = v;
            return this;
        }

        public Builder expireAt(Instant v) {
            this.expireAt = v;
            return this;
        }

        public Builder exDest(String v) {
            this.exDest = v;
            return this;
        }

        public Builder candidateVenues(List<VenueId> v) {
            this.candidateVenues = v == null ? List.of() : List.copyOf(v);
            return this;
        }

        public Builder targetChildQty(Long v) {
            this.targetChildQty = v;
            return this;
        }

        public Builder maxParallelChildren(int v) {
            this.maxParallelChildren = v;
            return this;
        }

        public Builder postOnly(boolean v) {
            this.postOnly = v;
            return this;
        }

        public Builder iocOnly(boolean v) {
            this.iocOnly = v;
            return this;
        }

        public Builder intentId(IntentId v) {
            this.intentId = v;
            return this;
        }

        public Builder intentRevision(int v) {
            this.intentRevision = v;
            return this;
        }

        public Builder tsNanos(long v) {
            this.tsNanos = v;
            return this;
        }

        public ParentRouteIntent build() {
            Objects.requireNonNull(parentId, "parentId");
            Objects.requireNonNull(clOrdId, "clOrdId");
            Objects.requireNonNull(instrumentKey, "instrumentKey");
            Objects.requireNonNull(side, "side");
            if (parentQty <= 0) throw new IllegalArgumentException("parentQty must be > 0");
            Objects.requireNonNull(ordType, "ordType");
            OrdTypeValidator.validate(ordType, limitPxMicros);
            Objects.requireNonNull(tif, "tif");
            Objects.requireNonNull(exDest, "DEFAULT"); // if you want SOR to always choose later, relax this
            Objects.requireNonNull(intentId, "intentId");
            if (maxParallelChildren <= 0) maxParallelChildren = 1;
            return new ParentRouteIntent(
                    parentId, clOrdId, accountId, accountType, instrumentKey, side, parentQty, leavesQty,
                    ordType, limitPxMicros, tif, expireAt, exDest, candidateVenues,
                    targetChildQty, maxParallelChildren, postOnly, iocOnly,
                    intentId, intentRevision, tsNanos
            );
        }
    }
}
