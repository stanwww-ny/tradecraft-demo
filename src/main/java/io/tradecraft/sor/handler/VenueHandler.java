package io.tradecraft.sor.handler;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.venue.event.VenueEvent;

public interface VenueHandler {
    void onVenue(Envelope<VenueEvent> e);
}
