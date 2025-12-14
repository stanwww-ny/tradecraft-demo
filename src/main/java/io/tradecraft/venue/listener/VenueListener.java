package io.tradecraft.venue.listener;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.venue.event.VenueEvent;

public interface VenueListener {
    void onEvent(Envelope<VenueEvent> e);
}
