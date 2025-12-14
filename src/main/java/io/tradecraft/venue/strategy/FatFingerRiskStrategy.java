package io.tradecraft.venue.strategy;

import io.tradecraft.common.domain.order.RejectReason;
import io.tradecraft.venue.api.VenueExecution;
import io.tradecraft.venue.api.VenueSupport;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.cmd.VenueCommand;
import io.tradecraft.venue.event.VenueReject;
import io.tradecraft.venue.nbbo.NbboSnapshot;

import java.util.Objects;

/**
 * Fat-finger price banding vs NBBO (micros-based).
 * <p>
 * Rules: - For LIMIT BUY: reject if limitPx > askPx * (1 + upPct) - For LIMIT SELL: reject if limitPx < bidPx * (1 -
 * downPct) - MARKET orders pass through (not this strategy’s concern)
 * <p>
 * Notes: - Uses VenueSupport.nbbo() for the snapshot (no direct provider coupling). - Returns VenueExecution.noop() to
 * allow later strategies to continue when within bands.
 */
public final class FatFingerRiskStrategy implements VenueStrategy {
    private final VenueSupport support;
    private final double upPct;    // e.g., 0.10 for +10% above ask (BUY)
    private final double downPct;  // e.g., 0.10 for -10% below bid (SELL)

    public FatFingerRiskStrategy(VenueSupport support, double upPct, double downPct) {
        this.support = Objects.requireNonNull(support, "support");
        if (upPct < 0.0 || downPct < 0.0) {
            throw new IllegalArgumentException("upPct/downPct must be non-negative");
        }
        this.upPct = upPct;
        this.downPct = downPct;
    }

    private static long pctUp(long base, double pct) {
        // round up conservatively; you can switch to Math.round if you prefer symmetric rounding
        return (long) Math.floor(base * (1.0 + pct));
    }

    private static long pctDown(long base, double pct) {
        return (long) Math.ceil(base * (1.0 - pct));
    }

    @Override
    public boolean appliesTo(VenueCommand command) {
        return command instanceof NewChildCmd;
    }

    @Override
    public VenueExecution decide(VenueCommand command) {
        if (!(command instanceof NewChildCmd n)) {
            // Should not be invoked if appliesTo() is respected by the caller.
            return VenueExecution.noop();
        }

        // Only LIMIT orders are checked here; MARKET orders pass to next strategy
        if (!n.isLimit()) {
            return VenueExecution.noop();
        }

        final Long limitPx = n.priceMicros();
        if (limitPx == null || limitPx <= 0) {
            return reject(n, RejectReason.INVALID_PRICE);
        }

        // Pull NBBO via VenueSupport (centralized dependency)
        final NbboSnapshot snap = support.nbboProvider().snapshot();

        // If NBBO is partially missing, let downstream decide (you may change this to a reject if desired)
        final boolean hasBid = snap != null && snap.hasBid();
        final boolean hasAsk = snap != null && snap.hasAsk();
        if (!hasBid && !hasAsk) {
            return VenueExecution.noop();
        }

        if (n.isBuy()) {
            if (!hasAsk) return VenueExecution.noop();
            final long ask = snap.askPxMicros();
            final long upThreshold = pctUp(ask, upPct);
            if (limitPx > upThreshold) {
                return reject(n, RejectReason.INVALID_PRICE);
            }
        } else {
            if (!hasBid) return VenueExecution.noop();
            final long bid = snap.bidPxMicros();
            final long downThreshold = pctDown(bid, downPct);
            if (limitPx < downThreshold) {
                return reject(n, RejectReason.INVALID_PRICE);
            }
        }

        // Within bands → allow later strategies to proceed
        return VenueExecution.noop();
    }

    // You can switch System.nanoTime() to support.clocks().nowNanos() if you expose it via VenueSupport
    private VenueExecution reject(NewChildCmd n, RejectReason rejectReason) {
        return VenueExecution.reject(VenueReject.builder()
                .childId(n.childId()).tsNanos(support.dualTimeSource().nowNanos()).rejectReason(rejectReason).build());
    }
}
