package io.tradecraft.common.id.allocator;

import io.tradecraft.common.id.VenueOrderId;
import io.tradecraft.common.id.generator.IdGenerator;

public final class VenueOrderIdAllocator {
    private final IdGenerator<String> generator;

    public VenueOrderIdAllocator(IdGenerator<String> generator) {
        this.generator = generator;
    }

    public VenueOrderId allocate() {
        return new VenueOrderId(generator.next());
    }
}
