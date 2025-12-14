package io.tradecraft.venue.api;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.venue.cmd.VenueCommand;

/*
SOR → Venue: VenueCmd (New/Cancel/Replace)
VenueSupport.Adapter: VenueCmd → VenueOrder (validate, enrich, idempotency)
VenueStrategy: apply(VenueOrder) → VenueExecution (uses MatchingEngine/OrderBook/NBBO as needed)
VenueSupport.Emitter: VenueExecution → List<VenueEvent> (Ack/PartialFill/Fill/Reject/CancelAck/CancelReject/ReplaceAck…)
Venue → SOR: publish VenueEvents on the outbound queue
 */
public interface Venue {
    VenueId id();

    void onCommand(Envelope<VenueCommand> envelope);

    default void start() {
    }

    default void stop() {
    }
}
