package io.tradecraft.sor.core;

import io.tradecraft.common.spi.sor.intent.CancelChildIntent;
import io.tradecraft.common.spi.sor.intent.NewChildIntent;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.sor.policy.VenuePolicy;
import io.tradecraft.sor.state.ChildState;
import io.tradecraft.sor.state.ChildStatus;
import io.tradecraft.venue.cmd.CancelChildCmd;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.cmd.VenueCommand;

import java.util.List;

public class DefaultChildIntentReducer implements ChildIntentReducer {
    private final VenuePolicy policy;
    public DefaultChildIntentReducer(VenuePolicy policy) {
        this.policy = policy;
    }

    @Override
    public SorEffects reduce(ChildState s, PubChildIntent ev) {
        return switch (ev) {
            case NewChildIntent i -> onNew(s, i);
            case CancelChildIntent c -> onCancel(s, c);
            // add: case ReplaceChildIntent r -> onReplace(s, r);
            default -> SorEffects.of(s); // no-op fallback
        };
    }

    public SorEffects onNew(ChildState s, NewChildIntent i) {
        if (s != null && s.status().isTerminal()) {
            return SorEffects.of(s);
        }
        var next = ChildState.builder(i.parentId(), i.childId(), i.tsNanos())
                .childClOrdId(i.childClOrdId())
                .venueId(i.venueId())          // null => smart-route; non-null => directed
                .side(i.side())
                .qty(i.qty())
                .ordType(i.ordType())
                .tif(i.tif())
                .expireAt(i.expiryAt())
                .status(ChildStatus.NEW_PENDING)
                .build();

        VenueCommand cmd = NewChildCmd.builder()
                .parentId(i.parentId())
                .childId(i.childId())
                .childClOrdId(i.childClOrdId())
                .accountId(i.accountId())
                .instrumentKey(i.instrumentKey())
                .side(i.side())
                .qty(i.qty())
                .ordType(i.ordType())
                .priceMicros(i.priceMicros()) // rename to limitPxMicros(...) if that’s your field
                .tif(i.tif())
                .venueId(i.venueId())
                .tsNanos(i.tsNanos())
                .build();

        return new SorEffects(next, List.of(cmd), List.of());
    }

    private SorEffects onCancel(ChildState s, CancelChildIntent c) {
        // Nothing to cancel
        if (s == null || s.status().isTerminal() || s.status() == ChildStatus.PENDING_CANCEL) {
            return SorEffects.of(s);
        }

        // Prefer a withStatus(...) helper to avoid toBuilder()
        var next = withStatus(s, ChildStatus.PENDING_CANCEL);

        var cmd = CancelChildCmd.builder()
                .parentId(s.parentId())
                .childId(s.childId())
                .childClOrdId(s.childClOrdId())
                .venueId(s.venueId())
                .venueOrderId(s.venueOrderId())
                .tsNanos(c.tsNanos()) // use cancel intent timestamp
                .build();

        return new SorEffects(next, List.of(cmd), List.of());
    }

    /** Small helper to rebuild ChildState with a different status, if you don’t have s.withStatus(). */
    private static ChildState withStatus(ChildState s, ChildStatus status) {
        if (s == null) return null;
        return ChildState.builder(s.parentId(), s.childId(), s.createdTsNanos())
                .childClOrdId(s.childClOrdId())
                .venueId(s.venueId())
                .side(s.side())
                .qty(s.qty())
                .ordType(s.ordType())
                .tif(s.tif())
                .expireAt(s.expireAt())
                .status(status)
                .build();
    }

}
