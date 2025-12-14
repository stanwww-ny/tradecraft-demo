package io.tradecraft.sor.core;

import io.tradecraft.sor.state.ChildState;
import io.tradecraft.venue.event.VenueEvent;

public interface ChildVenueReducer {
    SorEffects reduce(ChildState s, VenueEvent ev);
}
