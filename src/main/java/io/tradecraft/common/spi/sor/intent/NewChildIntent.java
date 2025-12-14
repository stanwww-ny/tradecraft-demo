package io.tradecraft.common.spi.sor.intent;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.IntentId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;

import java.time.Instant;

public record NewChildIntent(
        ParentId parentId,
        ChildId childId,
        ChildClOrdId childClOrdId,
        IntentId intentId,
        String accountId,
        DomainAccountType accountType,
        InstrumentKey instrumentKey,
        DomainSide side,
        long qty,
        DomainOrdType ordType,
        Long priceMicros,
        DomainTif tif,
        Instant expiryAt,
        VenueId venueId,   // null => smart-route; non-null => directed
        long tsNanos
) implements PubChildIntent {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ParentId parentId;
        private ChildId childId;
        private ChildClOrdId childClOrdId;
        private IntentId intentId;
        private String accountId;
        private DomainAccountType accountType;
        private InstrumentKey instrumentKey;
        private DomainSide side;
        private long qty;
        private DomainOrdType ordType;
        private Long priceMicros;
        private DomainTif tif;
        private Instant expiryAt;
        private VenueId venueId;
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

        public Builder childClOrdId(ChildClOrdId childClOrdId) {
            this.childClOrdId = childClOrdId;
            return this;
        }

        public Builder intentId(IntentId intentId) {
            this.intentId = intentId;
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

        public Builder qty(long qty) {
            this.qty = qty;
            return this;
        }

        public Builder ordType(DomainOrdType ordType) {
            this.ordType = ordType;
            return this;
        }

        public Builder priceMicros(Long priceMicros) {
            this.priceMicros = priceMicros;
            return this;
        }

        public Builder tif(DomainTif tif) {
            this.tif = tif;
            return this;
        }

        public Builder expiryAt(Instant expiryAt) {
            this.expiryAt = expiryAt;
            return this;
        }

        public Builder venueId(VenueId venueId) {
            this.venueId = venueId;
            return this;
        }

        public Builder tsNanos(long tsNanos) {
            this.tsNanos = tsNanos;
            return this;
        }

        public NewChildIntent build() {
            return new NewChildIntent(
                    parentId,
                    childId,
                    childClOrdId,
                    intentId,
                    accountId,
                    accountType,
                    instrumentKey,
                    side,
                    qty,
                    ordType,
                    priceMicros,
                    tif,
                    expiryAt,
                    venueId,
                    tsNanos
            );
        }
    }
}
