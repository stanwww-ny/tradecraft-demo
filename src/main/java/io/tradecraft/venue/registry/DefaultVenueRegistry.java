package io.tradecraft.venue.registry;

import io.micrometer.common.util.StringUtils;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.venue.api.Venue;

import java.util.Map;
import java.util.Objects;

/**
 * Thread-safe in-memory registry of venues. Construct with a VenueListener so built-in (default) venues can callback.
 */
public final class DefaultVenueRegistry implements VenueRegistry {
    private final Map<VenueId, Venue> venues;
    private final VenueId defaultVenueId;

    public DefaultVenueRegistry(Map<VenueId, Venue> venues, VenueId defaultVenueId) {
        if (venues == null || venues.isEmpty())
            throw new IllegalArgumentException("venues must be non-empty");
        this.venues = Map.copyOf(venues);                 // freeze at construction
        this.defaultVenueId = Objects.requireNonNull(defaultVenueId);
        if (!this.venues.containsKey(this.defaultVenueId))
            throw new IllegalArgumentException("defaultVenueId not in venues: " + defaultVenueId);
    }


    @Override
    public boolean hasVenue(String venueName) {
        if (!StringUtils.isBlank(venueName)) {
            VenueId venueId = VenueId.of(venueName);
            return venues.containsKey(venueId);
        }
        return false;
    }

    @Override
    public boolean hasVenue(VenueId venueId) {
        return venues.containsKey(venueId);
    }

    @Override
    public VenueId pickDefault() {
        return defaultVenueId;
    }

    @Override
    public Venue resolve(VenueId venueId) {
        return venues.get(venueId);
    }

    @Override
    public VenueId resolve(String venueName) {
        return VenueId.XNAS;
    }
}
