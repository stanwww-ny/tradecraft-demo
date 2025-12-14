package io.tradecraft.venue.integration;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMeta;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.JdkQueueChannel;
import io.tradecraft.common.id.IdFactory;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.venue.api.DefaultVenueSupport;
import io.tradecraft.venue.api.VenueSupport;
import io.tradecraft.venue.cmd.CancelChildCmd;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.cmd.VenueCommand;
import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueCancelDone;
import io.tradecraft.venue.event.VenueEvent;
import io.tradecraft.venue.event.VenueFill;
import io.tradecraft.venue.listener.TinyVenueListener;
import io.tradecraft.venue.listener.VenueListener;
import io.tradecraft.venue.matching.MatchingEngine;
import io.tradecraft.venue.nbbo.NbboCache;
import io.tradecraft.venue.nbbo.NbboProvider;
import io.tradecraft.venue.nbbo.NbboUpdater;
import io.tradecraft.venue.registry.DefaultVenue;
import io.tradecraft.venue.store.InMemoryVenueOrderRepository;
import io.tradecraft.venue.store.VenueOrderRepository;
import io.tradecraft.venue.strategy.MatchingEngineStrategy;
import io.tradecraft.venue.strategy.VenueStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultVenueEndToEndOrderBookIT {

    private static final AtomicLong SEQ = new AtomicLong(1);
    // ---------- Test wiring ----------
    private VenueId venueId;
    private EventQueue<Envelope<VenueEvent>> venueEvtBus;
    private VenueListener venueListener;
    private DefaultVenue venue;
    // NBBO cache (same instance exposed as provider & updater)
    private NbboCache nbbo;
    private NbboProvider nbboProvider;
    private NbboUpdater nbboUpdater;
    private IdFactory ids;
    private DualTimeSource dualTimeSource;
    private EnvelopeMetaFactory metaFactory;
    private EnvelopeMeta meta;

    // ---------- Order-book focussed tests ----------

    private static <T extends VenueEvent> T lastOf(List<VenueEvent> events, Class<T> type) {
        for (int i = events.size() - 1; i >= 0; i--) {
            var e = events.get(i);
            if (type.isInstance(e)) return type.cast(e);
        }
        return null;
    }

    @BeforeEach
    void setUp() {
        venueId = VenueId.XNAS;
        dualTimeSource = TestClocks.msTicker();
        ids = IdFactory.testIds(42L);
        metaFactory = new EnvelopeMetaFactory(ids.envelopeSeq(), dualTimeSource);
        meta = metaFactory.newMeta();
        venueEvtBus = new JdkQueueChannel<>();
        venueListener = new TinyVenueListener(venueEvtBus, metaFactory);

        nbbo = new NbboCache();
        nbboProvider = nbbo;
        nbboUpdater = nbbo;

        // Seed a WIDE NBBO so ImmediateFillStrategy does NOT trigger on limits:
        // e.g., 50.00 / 500.00 (micros)
        nbboUpdater.onTopOfBookUpdate(50_000_000L, 500_000_000L, dualTimeSource.nowNanos());

        VenueOrderRepository orderRepo = new InMemoryVenueOrderRepository(dualTimeSource);

        VenueSupport support = new DefaultVenueSupport(
                venueId, orderRepo, nbboProvider, venueListener, ids.venueOrder(), ids.exec(), metaFactory);

        MatchingEngine engine = new MatchingEngine(support);

        // Pipeline: risk → immediate (won’t act with wide NBBO) → engine (book)
        List<VenueStrategy> strategies = List.of(
                //new FatFingerRiskStrategy(support, 50, 50),
                //new ImmediateFillStrategy(support),
                new MatchingEngineStrategy(engine)
        );

        venue = new DefaultVenue(venueId, strategies, venueListener, metaFactory);
    }

    @Test
    void nonCrossableLimitBuy_rests_thenSellAtSamePrice_matches_onBook_fullFill() throws Exception {
        // Rest BUY @ 200.00 (not crossable vs ask=500.00)
        var buyRest = newLimit(DomainSide.BUY, 150, 200_000_000L);
        Envelope<VenueCommand> envelope1 = Envelope.of(buyRest, meta);
        venue.onCommand(envelope1);
        var evts1 = drainEvents(150);
        assertTrue(evts1.stream().anyMatch(e -> e instanceof VenueAck), "NEW BUY ack expected");
        assertTrue(evts1.stream().noneMatch(e -> e instanceof VenueFill), "BUY should rest, no fill yet");

        // Incoming SELL @ 200.00 should cross the resting BUY on the book (engine matches)
        var sellCross = newLimit(DomainSide.SELL, 150, 200_000_000L);
        Envelope<VenueCommand> envelope2 = Envelope.of(sellCross, meta);
        venue.onCommand(envelope2);

        var evts2 = drainEvents(200);
        var fill = lastOf(evts2, VenueFill.class);
        assertNotNull(fill, "Expected a fill from book match");
        assertEquals(200_000_000L, fill.lastPxMicros(), "Match price should be the order price (both are 200.00)");
        assertEquals(150L, fill.lastQty(), "All quantity should be matched");
    }

    // ---------- Helpers (events & command builders) ----------

    @Test
    void partialFill_restingBuy_thenCancelRemaining_emitsCancel() throws Exception {
        // Keep NBBO wide in @BeforeEach so ImmediateFill never triggers for LIMITs
        // and build venueId with MatchingEngineStrategy only for this IT.

        // 1) Rest BUY @ 200.00 qty 150
        var buyRest = newLimit(DomainSide.BUY, 150, 200_000_000L);
        Envelope<VenueCommand> envelope1 = Envelope.of(buyRest, meta);
        venue.onCommand(envelope1);
        drainEvents(120); // clear acks or any book-side notifications

        // 2) SELL @ 200.00 qty 100 → partial fill; 50 should remain on the book
        var sellPartial = newLimit(DomainSide.SELL, 100, 200_000_000L);
        Envelope<VenueCommand> envelope2 = Envelope.of(sellPartial, meta);
        venue.onCommand(envelope2);

        var evts2 = drainEvents(200);
        var fill = lastOf(evts2, VenueFill.class);
        assertNotNull(fill, "Expected a partial fill");
        assertEquals(200_000_000L, fill.lastPxMicros(), "Match price should be 200.00");
        assertEquals(100L, fill.lastQty(), "Partial should trade 100, leaving 50");

        // 3) CANCEL remaining BUY (targeting same childId)
        var cx = newCancelOf(buyRest);
        Envelope<VenueCommand> envelope3 = Envelope.of(cx, meta);
        venue.onCommand(envelope3);

        var evts3 = drainEvents(200);
        var cancel = lastOf(evts3, VenueCancelDone.class);
        assertNotNull(cancel, "Expected a cancel event for remaining qty");

        // Ensure cancel refers to the same child order
        try {
            var m = cancel.getClass().getMethod("childId");
            assertEquals(buyRest.childId(), m.invoke(cancel), "Cancel should target the resting order’s childId");
        } catch (ReflectiveOperationException ignore) {
            // if your event doesn’t expose childId, skip
        }

        // Optional: assert canceled/leaves qty if your model exposes it
        boolean assertedQty = false;
        try {
            var m = cancel.getClass().getMethod("canceledQty");
            assertEquals(50L, (long) m.invoke(cancel), "Should cancel remaining 50");
            assertedQty = true;
        } catch (ReflectiveOperationException ignore) { /* no-op */ }

        if (!assertedQty) {
            try {
                var m = cancel.getClass().getMethod("leavesQty");
                assertEquals(50L, (long) m.invoke(cancel), "Should report remaining 50 as leaves");
                assertedQty = true;
            } catch (ReflectiveOperationException ignore) { /* no-op */ }
        }

        // No fills should be produced by the cancel batch
        assertTrue(evts3.stream().noneMatch(e -> e instanceof VenueFill), "Cancel must not emit fills");
    }

    @Test
    void nonCrossableLimitSell_rests_thenBuyAtSamePrice_matches_onBook_fullFill() throws Exception {
        // Rest SELL @ 205.00 (not crossable vs bid=50.00)
        var sellRest = newLimit(DomainSide.SELL, 120, 205_000_000L);

        Envelope<VenueCommand> envelope = Envelope.of(sellRest, meta);
        venue.onCommand(envelope);

        var evts1 = drainEvents(150);
        assertTrue(evts1.stream().anyMatch(e -> e instanceof VenueAck), "NEW SELL ack expected");
        assertTrue(evts1.stream().noneMatch(e -> e instanceof VenueFill), "SELL should rest, no fill yet");

        // Incoming BUY @ 205.00 should cross on the book
        var buyCross = newLimit(DomainSide.BUY, 120, 205_000_000L);
        Envelope<VenueCommand> envelope2 = Envelope.of(buyCross, meta);
        venue.onCommand(envelope2);

        var evts2 = drainEvents(200);
        var fill = lastOf(evts2, VenueFill.class);
        assertNotNull(fill, "Expected a fill from book match");
        assertEquals(205_000_000L, fill.lastPxMicros());
        assertEquals(120L, fill.lastQty());
    }

    // ---------- Helpers: build commands directly with Builders ----------

    private List<VenueEvent> drainEvents(long timeoutMillis) throws InterruptedException {
        var out = new ArrayList<VenueEvent>();
        final long deadline = dualTimeSource.nowNanos() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (true) {
            long remaining = deadline - dualTimeSource.nowNanos();
            if (remaining <= 0) break;
            long slice = Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(10));

            Envelope<VenueEvent> envelope = venueEvtBus.poll();
            VenueEvent ev = null;
            if (envelope != null) {
                ev = envelope.payload();
            }
            if (ev == null) continue;
            out.add(ev);
            // opportunistic immediate drain
            Envelope<VenueEvent> more;
            while ((more = venueEvtBus.poll()) != null) {
                out.add(more.payload());
            }
        }
        return out;
    }

    private NewChildCmd newMarket(DomainSide side, long qty) {
        long n = SEQ.getAndIncrement();
        return NewChildCmd.builder()
                .parentId(ids.parent().allocate())
                .childId(ids.child().allocate())
                .childClOrdId(ids.childClOrd().next())
                .accountId("ACC-" + n)
                .domainAccountType(DomainAccountType.CUSTOMER)
                .instrumentKey(InstrumentKey.ofSymbol("ABC"))
                .side(side)
                .qty(qty)
                .ordType(DomainOrdType.MARKET)
                .tif(DomainTif.DAY)
                .venueId(venueId)
                .tsNanos(dualTimeSource.nowNanos())
                .build();
    }

    private NewChildCmd newLimit(DomainSide side, long qty, long limitPxMicros) {
        long n = SEQ.getAndIncrement();
        return NewChildCmd.builder()
                .parentId(ids.parent().allocate())
                .childId(ids.child().allocate())
                .childClOrdId(ids.childClOrd().next())
                .accountId("ACC-" + n)
                .domainAccountType(DomainAccountType.CUSTOMER)
                .instrumentKey(InstrumentKey.ofSymbol("ABC"))
                .side(side)
                .qty(qty)
                .ordType(DomainOrdType.LIMIT)
                // If your builder uses a different setter (e.g., limitPxMicros), change this line:
                .priceMicros(limitPxMicros)
                .tif(DomainTif.DAY)
                .venueId(venueId)
                .tsNanos(dualTimeSource.nowNanos())
                .build();
    }

    private CancelChildCmd newCancelOf(NewChildCmd newCmd) {
        long n = SEQ.getAndIncrement();
        return CancelChildCmd.builder()
                .parentId(newCmd.parentId())
                .childId(newCmd.childId())
                .childClOrdId(newCmd.childClOrdId())
                .instrumentKey(newCmd.instrumentKey())
                .venueId(venueId)
                .tsNanos(dualTimeSource.nowNanos())
                .build();
    }
}
