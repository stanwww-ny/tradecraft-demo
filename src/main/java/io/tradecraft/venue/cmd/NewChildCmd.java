package io.tradecraft.venue.cmd;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

public record NewChildCmd(
        ParentId parentId,
        ChildId childId,
        ChildClOrdId childClOrdId,
        String accountId,
        DomainAccountType domainAccountType,
        InstrumentKey instrumentKey,            // wire-friendly
        DomainSide side,
        long qty,
        DomainOrdType ordType,
        Long priceMicros,            // 0 for market
        DomainTif tif,
        VenueId venueId,
        long tsNanos
) implements VenueCommand {
    public static Builder builder() {
        return new Builder();
    }

    public boolean isBuy() {
        // Example: DomainSide.BUY / SELL
        // return n.side() == DomainSide.BUY;
        return side().isBuy();
    }

    public boolean isMarket() {
        // Example: DomainOrdType.MARKET
        // return n.ordType() == DomainOrdType.MARKET;
        return ordType() == DomainOrdType.MARKET;
    }

    // ---- Adapt these two blocks if your domain enums/fields are named differently ----

    public boolean isLimit() {
        // Example: DomainOrdType.LIMIT
        // return n.ordType() == DomainOrdType.LIMIT;
        return ordType() == DomainOrdType.LIMIT;
    }

    public Long limitPxMicrosOrNull() {
        // Return null if not present (e.g., market order)
        return ordType() == DomainOrdType.LIMIT ? priceMicros() : null;
    }

    public static final class Builder {
        private ParentId parentId;
        private ChildId childId;
        private ChildClOrdId childClOrdId;
        private String accountId;
        private DomainAccountType domainAccountType;
        private InstrumentKey instrumentKey;
        private DomainSide side;
        private long qty;
        private DomainOrdType ordType;
        private Long priceMicros;
        private DomainTif tif;
        private VenueId venueId;
        private VenueOrderId venueOrderId;
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

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder domainAccountType(DomainAccountType domainAccountType) {
            this.domainAccountType = domainAccountType;
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

        public Builder venueId(VenueId venueId) {
            this.venueId = venueId;
            return this;
        }

        public Builder venueOrderId(VenueOrderId venueOrderId) {
            this.venueOrderId = venueOrderId;
            return this;
        }

        public Builder tsNanos(long tsNanos) {
            this.tsNanos = tsNanos;
            return this;
        }

        public NewChildCmd build() {
            return new NewChildCmd(
                    parentId,
                    childId,
                    childClOrdId,
                    accountId,
                    domainAccountType,
                    instrumentKey,
                    side,
                    qty,
                    ordType,
                    priceMicros,
                    tif,
                    venueId,
                    tsNanos
            );
        }
    }

}

