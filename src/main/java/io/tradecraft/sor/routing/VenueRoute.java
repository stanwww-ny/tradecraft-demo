package io.tradecraft.sor.routing;

import io.tradecraft.common.id.VenueId;

import java.util.Objects;

public record VenueRoute(
        VenueId venueId,
        long qty
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private VenueId venueId;
        private long qty;

        public Builder venueId(VenueId venueId) {
            this.venueId = venueId;
            return this;
        }

        public Builder qty(long qty) {
            this.qty = qty;
            return this;
        }

        public VenueRoute build() {
            Objects.requireNonNull(venueId, "venueId");
            if (qty <= 0) throw new IllegalStateException("qty must be > 0");
            return new VenueRoute(venueId, qty);
        }
    }
}

