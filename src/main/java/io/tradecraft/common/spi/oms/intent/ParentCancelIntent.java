package io.tradecraft.common.spi.oms.intent;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.IntentId;
import io.tradecraft.common.id.ParentId;

import java.time.Instant;
import java.util.List;

public record ParentCancelIntent(
        ParentId parentId,
        ClOrdId clOrdId,
        ChildId childId,
        ChildClOrdId childClOrdId,
        String accountId,
        DomainAccountType accountType,
        InstrumentKey instrumentKey,
        DomainSide side,
        long parentQty,
        long leavesQty,
        DomainOrdType ordType,
        Long limitPxMicros,
        DomainTif tif,
        Instant expireAt,
        String exDest,
        List<String> candidateVenues,
        Long targetChildQty,
        int maxParallelChildren,
        boolean postOnly,
        boolean iocOnly,
        IntentId intentId,
        int intentRevision,
        long tsNanos
) implements PubParentIntent {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private ClOrdId clOrdId;
        private ChildId childId;
        private ChildClOrdId childClOrdId;
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
        private List<String> candidateVenues;
        private Long targetChildQty;
        private int maxParallelChildren;
        private boolean postOnly;
        private boolean iocOnly;
        private IntentId intentId;
        private int intentRevision;
        private long tsNanos;

        public Builder parentId(ParentId parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder clOrdId(ClOrdId clOrdId) {
            this.clOrdId = clOrdId;
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

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder accountType(DomainAccountType accountType) {
            this.accountType = accountType;
            return this;
        }

        public Builder instrumentKey(InstrumentKey instrumentKey) {
            this.instrumentKey = instrumentKey;
            return this;
        }

        public Builder side(DomainSide side) {
            this.side = side;
            return this;
        }

        public Builder parentQty(long parentQty) {
            this.parentQty = parentQty;
            return this;
        }

        public Builder leavesQty(long leavesQty) {
            this.leavesQty = leavesQty;
            return this;
        }

        public Builder ordType(DomainOrdType ordType) {
            this.ordType = ordType;
            return this;
        }

        public Builder limitPxMicros(Long limitPxMicros) {
            this.limitPxMicros = limitPxMicros;
            return this;
        }

        public Builder tif(DomainTif tif) {
            this.tif = tif;
            return this;
        }

        public Builder expireAt(Instant expireAt) {
            this.expireAt = expireAt;
            return this;
        }

        public Builder exDest(String exDest) {
            this.exDest = exDest;
            return this;
        }

        public Builder candidateVenues(List<String> candidateVenues) {
            this.candidateVenues = candidateVenues;
            return this;
        }

        public Builder targetChildQty(Long targetChildQty) {
            this.targetChildQty = targetChildQty;
            return this;
        }

        public Builder maxParallelChildren(int maxParallelChildren) {
            this.maxParallelChildren = maxParallelChildren;
            return this;
        }

        public Builder postOnly(boolean postOnly) {
            this.postOnly = postOnly;
            return this;
        }

        public Builder iocOnly(boolean iocOnly) {
            this.iocOnly = iocOnly;
            return this;
        }

        public Builder intentId(IntentId intentId) {
            this.intentId = intentId;
            return this;
        }

        public Builder intentRevision(int intentRevision) {
            this.intentRevision = intentRevision;
            return this;
        }

        public Builder tsNanos(long tsNanos) {
            this.tsNanos = tsNanos;
            return this;
        }

        public ParentCancelIntent build() {
            return new ParentCancelIntent(
                    parentId,
                    clOrdId,
                    childId,
                    childClOrdId,
                    accountId,
                    accountType,
                    instrumentKey,
                    side,
                    parentQty,
                    leavesQty,
                    ordType,
                    limitPxMicros,
                    tif,
                    expireAt,
                    exDest,
                    candidateVenues,
                    targetChildQty,
                    maxParallelChildren,
                    postOnly,
                    iocOnly,
                    intentId,
                    intentRevision,
                    tsNanos
            );
        }
    }
}
