package io.tradecraft.venue.strategy;

import io.tradecraft.venue.api.VenueExecution;
import io.tradecraft.venue.api.VenueSupport;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.cmd.VenueCommand;
import io.tradecraft.venue.matching.orderbook.FillSource;
import io.tradecraft.venue.model.VenueOrder;
import io.tradecraft.venue.pricing.CrossableLimitTopOfBookRule;
import io.tradecraft.venue.pricing.PriceRule;
import io.tradecraft.venue.pricing.TopOfBookRule;

import java.util.List;

public final class ImmediateFillStrategy implements VenueStrategy {
    private final VenueSupport support;
    private final List<PriceRule> priceRules;

    public ImmediateFillStrategy(VenueSupport support) {
        this.support = support;
        this.priceRules = List.of(
                new CrossableLimitTopOfBookRule(support.nbboProvider()),
                new TopOfBookRule(support.nbboProvider())
        );
    }

    @Override
    public boolean appliesTo(VenueCommand cmd) {
        // e.g., handle Market/IOC via immediate; leave others to matching
        if (cmd instanceof NewChildCmd n) {
            if (n.ordType().isMarket()) return true;

            if (n.ordType().isLimit()) {
                var snap = support.nbboProvider().snapshot();
                Long bid = snap.bidPxMicros();
                Long ask = snap.askPxMicros();

                if (n.side().isBuy() && ask != null && n.priceMicros() != null && n.priceMicros() >= ask) return true;
                return n.side().isSell() && bid != null && n.priceMicros() != null && n.priceMicros() <= bid;
            }
            return false;
        }
        return false;
    }

    @Override
    public VenueExecution decide(VenueCommand cmd) {
        if (cmd instanceof NewChildCmd n) {
            return executeNew(n);
        } else {
            throw new IllegalArgumentException("Unsupported: " + cmd);
        }
    }

    private VenueExecution executeNew(NewChildCmd c) {
        // freeze NBBO for this command (optional but good)
        final var snap = support.nbboProvider().snapshot();

        final VenueOrder vo = support.create(c);

        PriceRule matched = null;
        long px = 0L;

        for (var r : priceRules) {
            // if your rule API doesn't take a snapshot, keep using provider internally;
            // if possible, prefer r.appliesTo(c, vo, snap) & r.priceMicros(c, vo, snap)
            if (r.appliesTo(c, vo)) {
                matched = r;
                px = r.priceMicros(c, vo);
                break; // first-match wins due to ordered `priceRules`
            }
        }

        if (matched == null) {
            // This strategy doesn't apply; let MatchingEngineStrategy handle it
            return VenueExecution.noop();
        }

        var acks = List.of(support.ack(c, vo));
        var fill = support.applyFill(vo, vo.qty(), px, true, FillSource.MATCHING_ENGINE);
        return VenueExecution.events(acks, List.of(fill));
    }
}

