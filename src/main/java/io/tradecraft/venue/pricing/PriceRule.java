package io.tradecraft.venue.pricing;

import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.model.VenueOrder;

@FunctionalInterface
public interface PriceRule {
    boolean appliesTo(NewChildCmd c, VenueOrder vo);

    default long priceMicros(NewChildCmd c, VenueOrder vo) {
        throw new UnsupportedOperationException();
    }
}
