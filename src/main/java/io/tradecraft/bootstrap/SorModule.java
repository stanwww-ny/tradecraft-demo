package io.tradecraft.bootstrap;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.id.allocator.ChildIdAllocator;
import io.tradecraft.common.id.generator.ChildClOrdIdGenerator;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;
import io.tradecraft.sor.SorEngine;
import io.tradecraft.sor.routing.VenueRouter;
import io.tradecraft.sor.store.ChildStateStore;
import io.tradecraft.sor.store.ChildCtxStore;
import io.tradecraft.sor.store.DefaultChildStateStore;
import io.tradecraft.sor.store.InMemoryChildCtxStore;
import io.tradecraft.venue.event.VenueEvent;

public final class SorModule implements Lifecycle {
    private final EventQueue<Envelope<OrderEvent>> sorEventBus;
    private final EventQueue<Envelope<PubParentIntent>> parentIntentBus;
    private final EventQueue<Envelope<PubChildIntent>> childIntentBus;
    private final EventQueue<Envelope<VenueEvent>> venueEventBus;
    private final EnvelopeMetaFactory metaFactory;
    private final VenueRouter venueRouter;
    private final ChildIdAllocator childIdAllocator;
    private final ChildClOrdIdGenerator childClOrdIdGenerator;
    private SorEngine sorEngine;

    public SorModule(EventQueue<Envelope<OrderEvent>> sorEventBus,
                     EventQueue<Envelope<PubParentIntent>> parentIntentBus,
                     EventQueue<Envelope<PubChildIntent>> childIntentBus,
                     EventQueue<Envelope<VenueEvent>> venueEventBus,
                     VenueRouter venueRouter,
                     ChildIdAllocator childIdAllocator,
                     ChildClOrdIdGenerator childClOrdIdGenerator,
                     EnvelopeMetaFactory metaFactory) {
        this.sorEventBus = sorEventBus;
        this.parentIntentBus = parentIntentBus;
        this.childIntentBus = childIntentBus;
        this.venueEventBus = venueEventBus;
        this.venueRouter = venueRouter;
        this.childIdAllocator = childIdAllocator;
        this.childClOrdIdGenerator = childClOrdIdGenerator;
        this.metaFactory = metaFactory;
    }

    @Override
    public void start() {
        ChildCtxStore childCtxStore = new InMemoryChildCtxStore();
        ChildStateStore childStateStore = new DefaultChildStateStore();

        sorEngine = new SorEngine(parentIntentBus, childIntentBus, sorEventBus, venueEventBus,
                venueRouter, childCtxStore, childStateStore, childIdAllocator, childClOrdIdGenerator, metaFactory);
        sorEngine.start();
    }

    @Override
    public void stop() {
        if (sorEngine != null) sorEngine.stop();
    }
}

