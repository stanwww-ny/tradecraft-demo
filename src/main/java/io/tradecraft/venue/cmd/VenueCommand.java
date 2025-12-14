package io.tradecraft.venue.cmd;

import io.tradecraft.common.id.VenueId;

/**
 * Commands issued by SOR to venues.
 */
public sealed interface VenueCommand
        permits CancelChildCmd, NewChildCmd, ReplaceChildCmd {
    VenueId venueId();
}
