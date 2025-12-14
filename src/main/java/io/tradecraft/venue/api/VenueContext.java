package io.tradecraft.venue.api;

import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.allocator.VenueOrderIdAllocator;
import io.tradecraft.venue.listener.VenueListener;
import io.tradecraft.venue.nbbo.NbboProvider;
import io.tradecraft.venue.store.VenueOrderRepository;

public interface VenueContext {
    VenueId venueId();

    NbboProvider nbboProvider();

    VenueOrderRepository orderRepository();

    VenueOrderIdAllocator venueOrderIdAllocator();

    VenueListener listener();

    DualTimeSource dualTimeSource();
}
