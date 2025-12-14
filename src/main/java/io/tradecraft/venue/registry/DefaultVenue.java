package io.tradecraft.venue.registry;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.log.LogUtils;
import io.tradecraft.venue.api.Venue;
import io.tradecraft.venue.api.VenueExecution;
import io.tradecraft.venue.cmd.VenueCommand;
import io.tradecraft.venue.listener.VenueListener;
import io.tradecraft.venue.strategy.VenueStrategy;

import java.util.List;
import java.util.Objects;

import static io.tradecraft.common.envelope.Stage.VENUE_ACK_SENT;
import static io.tradecraft.common.envelope.Stage.VENUE_ACCEPTED;
import static io.tradecraft.common.envelope.Stage.VENUE_CANCEL_SENT;
import static io.tradecraft.common.envelope.Stage.VENUE_FILLED_SENT;
import static io.tradecraft.common.envelope.Stage.VENUE_REPLACE_SENT;
import static io.tradecraft.common.meta.Flow.IN;
import static io.tradecraft.common.meta.Flow.OUT;
import static io.tradecraft.common.meta.MessageType.CMD;
import static io.tradecraft.common.meta.Component.VENUE;

/*
[SOR/OMS] → DefaultVenue (Controller-ish)
                 ├─ FatFingerRiskStrategy (Service)
                 ├─ ImmediateFillStrategy (Service)
                 └─ MatchingEngineStrategy (Service → Engine)
       └─ VenueSupport (Utilities/Factories, like DAO)
       └─ VenueListener (Outbound Port)
 */
public final class DefaultVenue implements Venue {

    private final List<VenueStrategy> strategies;
    private final VenueListener listener;
    private final VenueId venueId;
    private final EnvelopeMetaFactory metaFactory;

    public DefaultVenue(VenueId venueId, List<VenueStrategy> strategies, VenueListener listener, EnvelopeMetaFactory metaFactory) {
        this.venueId = Objects.requireNonNull(venueId);
        if (strategies.isEmpty()) throw new IllegalArgumentException("No strategies configured");
        this.strategies = List.copyOf(strategies);
        this.listener = Objects.requireNonNull(listener);
        this.metaFactory = Objects.requireNonNull(metaFactory);
    }

    @Override
    public VenueId id() {
        return venueId;
    }

    @Override
    public void onCommand(Envelope<VenueCommand> envelope) {
        VenueCommand cmd = envelope.payload();
        Meta meta = envelope.meta();
        metaFactory.addHop(meta, cmd);
        LogUtils.log(VENUE, CMD, IN, this, cmd);

        VenueExecution acc = VenueExecution.noop();
        boolean anyMatched = false;

        for (VenueStrategy s : strategies) {
            if (!s.appliesTo(cmd)) continue;
            anyMatched = true;

            final VenueExecution exec = s.decide(cmd);
            acc = acc.merge(exec); // your merge/plus from earlier

            // Stop the chain if terminal: reject/cancel or any fills produced
            if (exec.rejectOptional().isPresent()
                    || exec.cancelOptional().isPresent()
                    || !exec.fills().isEmpty()) {
                break;
            }
            // else continue to next strategy
        }

        if (!anyMatched) {
            throw new IllegalStateException("No strategy matches: " + cmd);
        }

        // Emit once, in order
        metaFactory.addHop(meta, VENUE_ACCEPTED);
        if (!acc.acks().isEmpty()) {
            acc.acks().forEach(a -> {
                metaFactory.addHop(meta, VENUE_ACK_SENT);
                LogUtils.log(VENUE, CMD, OUT, this, a);
                listener.onEvent(Envelope.of(a, meta));
            });
        }
        if (!acc.fills().isEmpty()) {
            acc.fills().forEach(f -> {
                metaFactory.addHop(meta, VENUE_FILLED_SENT);
                LogUtils.log(VENUE, CMD, OUT, this, f);
                listener.onEvent(Envelope.of(f, meta));
            });
        }
        acc.cancelOptional().ifPresent(c -> {
            metaFactory.addHop(meta, VENUE_CANCEL_SENT);
            LogUtils.log(VENUE, CMD, OUT, this, c);
            listener.onEvent(Envelope.of(c, meta));
        });
        acc.rejectOptional().ifPresent(r -> {
            metaFactory.addHop(meta, VENUE_REPLACE_SENT);
            LogUtils.log(VENUE, CMD, OUT, this, r);
            listener.onEvent(Envelope.of(r, meta));
        });
    }


    @Override
    public String toString() {
        return "DefaultVenue";
    }
}
