package io.tradecraft.venue.registry;

import io.tradecraft.common.id.VenueId;
import io.tradecraft.venue.api.Venue;

public interface VenueRegistry {
    boolean hasVenue(String venueIdName);

    boolean hasVenue(VenueId venueId);

    VenueId pickDefault();

    VenueId resolve(String venueIdName);

    Venue resolve(VenueId venueId);
}
