package io.tradecraft.venue.pricing;

import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.model.VenueOrder;
import io.tradecraft.venue.nbbo.NbboProvider;

public final class TopOfBookRule implements PriceRule {
    private final NbboProvider nbboProvider;

    public TopOfBookRule(NbboProvider nbboProvider) {
        this.nbboProvider = nbboProvider;
    }

    @Override
    public boolean appliesTo(NewChildCmd c, VenueOrder vo) {
        if (!c.ordType().isMarket()) return false;

        var snap = nbboProvider.snapshot();
        return (c.side().isBuy() && snap.askPxMicros() != null) ||
                (c.side().isSell() && snap.bidPxMicros() != null);
    }

    @Override
    public long priceMicros(NewChildCmd c, VenueOrder vo) {
        var snap = nbboProvider.snapshot();
        if (c.side().isBuy()) {
            return snap.askPxMicros(); // lift the ask
        } else {
            return snap.bidPxMicros(); // hit the bid
        }
    }
}
