package io.tradecraft.common.id.generator;

import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates venueId-assigned order IDs.
 * <p>
 * In real venues this could be numeric or alphanumeric. For simulation, we use an incrementing long.
 */
public final class VenueOrderIdGenerator {

    private final AtomicLong seq = new AtomicLong(1L);
    private final VenueId venueId;

    public VenueOrderIdGenerator(VenueId venueId) {
        this.venueId = venueId;
    }

    /**
     * Generate the next VenueOrderId as a String. Example: "V12345"
     */
    public VenueOrderId nextId() {
        long id = seq.getAndIncrement();
        return new VenueOrderId(venueId.value() + "-" + id);
    }

    /**
     * Reset the generator (useful for tests).
     */
    public void reset() {
        seq.set(1L);
    }
}

