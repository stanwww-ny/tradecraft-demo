package io.tradecraft.venue.pricing;

import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.model.VenueOrder;
import io.tradecraft.venue.nbbo.NbboProvider;

import java.util.Objects;

public final class CrossableLimitTopOfBookRule implements PriceRule {
    private final NbboProvider nbboProvider;

    public CrossableLimitTopOfBookRule(NbboProvider nbboProvider) {
        this.nbboProvider = Objects.requireNonNull(nbboProvider);
    }

    @Override
    public boolean appliesTo(NewChildCmd c, VenueOrder vo) {
        if (!c.ordType().isLimit() || c.priceMicros() == null || c.priceMicros() <= 0) return false;

        var snap = nbboProvider.snapshot(); // FRESH each time
        Long bid = snap.bidPxMicros();
        Long ask = snap.askPxMicros();

        if (c.side().isBuy()) {
            return ask != null && c.priceMicros() >= ask; // crossable BUY
        } else {
            return bid != null && c.priceMicros() <= bid; // crossable SELL  ✅ fixed inequality
        }
    }

    @Override
    public long priceMicros(NewChildCmd c, VenueOrder vo) {
        var snap = nbboProvider.snapshot(); // FRESH again (or cache within decide)
        if (c.side().isBuy()) {
            Long ask = snap.askPxMicros();
            if (ask == null) throw new IllegalStateException("No ask for crossable BUY");
            return ask; // execute at ask (lift)
        } else {
            Long bid = snap.bidPxMicros();
            if (bid == null) throw new IllegalStateException("No bid for crossable SELL");
            return bid; // execute at bid (hit)
        }
    }
}

