package io.tradecraft.sor;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.id.allocator.ChildIdAllocator;
import io.tradecraft.common.id.generator.ChildClOrdIdGenerator;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.common.utils.IdleStrategy;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;
import io.tradecraft.sor.handler.ChildIntentHandler;
import io.tradecraft.sor.handler.DefaultChildIntentHandler;
import io.tradecraft.sor.handler.DefaultParentIntentHandler;
import io.tradecraft.sor.handler.DefaultVenueHandler;
import io.tradecraft.sor.handler.ParentIntentHandler;
import io.tradecraft.sor.handler.VenueHandler;
import io.tradecraft.sor.routing.VenueRouter;
import io.tradecraft.sor.store.ChildStateStore;
import io.tradecraft.sor.store.ChildCtxStore;
import io.tradecraft.venue.event.VenueEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * SorEngine - Consumes OMS→SOR intents (PubChildIntent.*Intent) - Chooses venueId (via Router; default kept INSIDE the
 * engine) - Emits SOR→Venue commands (SorCommand.*Cmd) to venueId(s) via VenueRegister
 */
public final class SorEngine implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SorEngine.class);

    /**
     * Inbound OMS→SOR intents
     */
    private final EventQueue<Envelope<PubParentIntent>> parentIntentBus;
    private final EventQueue<Envelope<PubChildIntent>> childIntentBus;
    private final EventQueue<Envelope<VenueEvent>> venueEventBus; // internal

    private final ParentIntentHandler parentIntentHandler;
    private final ChildIntentHandler childIntentHandler;
    private final VenueHandler venueHandler;

    IdleStrategy idle = IdleStrategy.defaultStrategy();

    private volatile boolean running = true;
    private Thread worker;


    /* === Constructors === */

    /**
     * MVP constructor: Router is INTERNAL (DefaultRouter), no extra wiring needed.
     */
    public SorEngine(EventQueue<Envelope<PubParentIntent>> parentIntentBus,
                     EventQueue<Envelope<PubChildIntent>> childIntentBus,
                     EventQueue<Envelope<OrderEvent>> sorEventBus,
                     EventQueue<Envelope<VenueEvent>> venueEventBus, // internal
                     VenueRouter venueRouter,
                     ChildCtxStore childCtxStore,
                     ChildStateStore childStateStore,
                     ChildIdAllocator childIdAllocator,
                     ChildClOrdIdGenerator childClOrdIdGenerator,
                     EnvelopeMetaFactory metaFactory) {
        this.parentIntentBus = Objects.requireNonNull(parentIntentBus, "inboundIntentBus");
        this.childIntentBus = Objects.requireNonNull(childIntentBus, "childIntentBus");
        this.venueEventBus = Objects.requireNonNull(venueEventBus, "venueEventBus");
        this.parentIntentHandler = new DefaultParentIntentHandler(childIntentBus, venueRouter, childIdAllocator, childClOrdIdGenerator, metaFactory);
        this.childIntentHandler = new DefaultChildIntentHandler(venueRouter, childCtxStore, childStateStore, metaFactory);
        this.venueHandler = new DefaultVenueHandler(sorEventBus, childStateStore, metaFactory);
    }

    /* === Lifecycle === */

    public void start() {
        if (worker != null) return;
        worker = new Thread(this, "sor-engine");
        worker.start();
        log.info("SorEngine started");
    }

    public void stop() {
        running = false;
        if (worker != null) worker.interrupt();
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                boolean progressed = false;
                var parentIntentEnvelope = parentIntentBus.poll();
                if (parentIntentEnvelope != null) {
                    parentIntentHandler.onIntent(parentIntentEnvelope);
                    progressed = true;
                }

                var childIntentEnvelope = childIntentBus.poll();
                if (childIntentEnvelope != null) {
                    childIntentHandler.onIntent(childIntentEnvelope);
                    progressed = true;
                }

                var venueEventEnvelope = venueEventBus.poll();
                if (venueEventEnvelope != null) {
                    venueHandler.onVenue(venueEventEnvelope);
                    progressed = true;
                }

                if (!progressed) {
                    idle.idle();
                } else {
                    idle.reset();
                }

            } catch (Throwable t) {
                log.error("Unhandled in SorEngine loop", t);
            }
        }
        log.info("SorEngine stopped");
    }
}