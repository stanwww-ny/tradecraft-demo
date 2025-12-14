package io.tradecraft.venue;

import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.allocator.VenueOrderIdAllocator;
import io.tradecraft.common.id.generator.ExecIdGenerator;
import io.tradecraft.venue.api.DefaultVenueSupport;
import io.tradecraft.venue.api.Venue;
import io.tradecraft.venue.listener.VenueListener;
import io.tradecraft.venue.matching.MatchingEngine;
import io.tradecraft.venue.nbbo.NbboCache;
import io.tradecraft.venue.registry.DefaultVenue;
import io.tradecraft.venue.store.InMemoryVenueOrderRepository;
import io.tradecraft.venue.store.VenueOrderRepository;
import io.tradecraft.venue.strategy.FatFingerRiskStrategy;
import io.tradecraft.venue.strategy.ImmediateFillStrategy;
import io.tradecraft.venue.strategy.MatchingEngineStrategy;
import io.tradecraft.venue.strategy.VenueStrategy;

import java.util.List;

public class VenueFactory {
    public Venue createVenue(
            VenueId venueId,
            VenueListener listener,
            VenueOrderIdAllocator venueOrderIdAllocator,
            ExecIdGenerator execIdGenerator,
            EnvelopeMetaFactory metaFactory,
            NbboCache nbbo,
            DualTimeSource timeSource
    ) {
        VenueOrderRepository repo =
                new InMemoryVenueOrderRepository(timeSource);

        DefaultVenueSupport support =
                new DefaultVenueSupport(
                        venueId, repo, nbbo, listener,
                        venueOrderIdAllocator, execIdGenerator, metaFactory
                );

        MatchingEngine engine = new MatchingEngine(support);

        List<VenueStrategy> strategies = List.of(
                new FatFingerRiskStrategy(support, 50, 50),
                new ImmediateFillStrategy(support),
                new MatchingEngineStrategy(engine)
        );

        return new DefaultVenue(venueId, strategies, listener, metaFactory);
    }
}
