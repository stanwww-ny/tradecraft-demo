package io.tradecraft.sor.routing;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.spi.oms.intent.ParentRouteIntent;
import io.tradecraft.venue.cmd.VenueCommand;

/**
 * Router purpose: choose venueId id for a given intent. Default uses exDest or a register default.
 */
public interface VenueRouter {
    VenueRoutePlan venueRoutePlan(ParentRouteIntent intent);
    void dispatch(Envelope<VenueCommand> cmd);
}
