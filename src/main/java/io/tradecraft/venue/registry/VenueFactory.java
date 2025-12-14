package io.tradecraft.venue.registry;

import io.tradecraft.common.id.VenueId;
import io.tradecraft.venue.api.DefaultVenueSupport;
import io.tradecraft.venue.api.Venue;

public interface VenueFactory {
    Venue create(VenueId venueId, DefaultVenueSupport support);
}
