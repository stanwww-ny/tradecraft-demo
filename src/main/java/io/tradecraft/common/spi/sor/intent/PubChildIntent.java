package io.tradecraft.common.spi.sor.intent;

import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.spi.Outgoing;

/**
 * Intents published by OMS FSM to the SOR.
 */
public sealed interface PubChildIntent extends Outgoing
        permits NewChildIntent,
        CancelChildIntent {
    VenueId venueId();
}
