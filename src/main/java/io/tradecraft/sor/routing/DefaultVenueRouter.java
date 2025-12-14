package io.tradecraft.sor.routing;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.spi.oms.intent.ParentRouteIntent;
import io.tradecraft.venue.api.Venue;
import io.tradecraft.venue.cmd.VenueCommand;
import io.tradecraft.venue.registry.VenueRegistry;

import java.util.Objects;

// Demo router:
// - routing decides venue + quantity only
// - pricing and execution semantics are handled later
public final class DefaultVenueRouter implements VenueRouter {
    private final VenueRegistry venueRegistry;
    private final VenueId defaultVenue;

    public DefaultVenueRouter(VenueRegistry venueRegistry, VenueId defaultVenue) {
        this.venueRegistry = Objects.requireNonNull(venueRegistry, "venueRegistry");
        this.defaultVenue = Objects.requireNonNull(defaultVenue, "defaultVenue");
    }

    public VenueRoutePlan venueRoutePlan(ParentRouteIntent intent) {
        VenueId venueId = resolveVenue(intent);

        return VenueRoutePlan.builder(intent.intentId())
                .addRoute(VenueRoute.builder().venueId(venueId).qty(intent.parentQty()).build())
                .build();
    }

    private VenueId resolveVenue(ParentRouteIntent intent) {
        if (this.venueRegistry.hasVenue(intent.exDest())) {
           return this.venueRegistry.resolve(intent.exDest());
        }
        else {
            return this.defaultVenue;
        }
    }

    public void dispatch(Envelope<VenueCommand> envelope) {
        VenueCommand cmd = envelope.payload();
        final VenueId venueId = cmd.venueId();
        Venue venue = venueRegistry.resolve(venueId);
        venue.onCommand(envelope);
    }

}
