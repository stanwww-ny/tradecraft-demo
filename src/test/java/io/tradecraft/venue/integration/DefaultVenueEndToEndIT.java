package io.tradecraft.venue.integration;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.IdFactory;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.generator.VenueOrderIdGenerator;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.venue.api.DefaultVenueSupport;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueEvent;
import io.tradecraft.venue.event.VenueFill;
import io.tradecraft.venue.listener.VenueListener;
import io.tradecraft.venue.matching.MatchingEngine;
import io.tradecraft.venue.nbbo.NbboCache;
import io.tradecraft.venue.nbbo.NbboProvider;
import io.tradecraft.venue.nbbo.NbboUpdater;
import io.tradecraft.venue.registry.DefaultVenue;
import io.tradecraft.venue.store.InMemoryVenueOrderRepository;
import io.tradecraft.venue.store.VenueOrderRepository;
import io.tradecraft.venue.strategy.FatFingerRiskStrategy;
import io.tradecraft.venue.strategy.ImmediateFillStrategy;
import io.tradecraft.venue.strategy.MatchingEngineStrategy;
import io.tradecraft.venue.strategy.VenueStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration tests for DefaultVenue using concrete components: - InMemoryVenueOrderRepository -
 * SimpleNbboProvider - VenueOrderIdGenerator - DefaultVenueSupport - FatFingerRiskStrategy, ImmediateFillStrategy,
 * MatchingEngineStrategy - Capturing listener to verify side-effects
 */
class DefaultVenueEndToEndIT {

    private DualTimeSource dualTimeSource = TestClocks.msTicker();
    private NbboCache nbbo;
    private NbboProvider nbboProvider;
    private NbboUpdater nbboUpdater;
    private VenueId venueId;
    private VenueOrderRepository repo;
    private VenueOrderIdGenerator idGen;
    private CapturingListener listener;
    private DefaultVenueSupport support;
    private DefaultVenue venue;
    private IdFactory ids;
    private EnvelopeMetaFactory metaFactory;
    private Meta meta;

    @BeforeEach
    void setUp() {
        ids = IdFactory.testIds(42L);
        venueId = VenueId.XNAS;
        metaFactory = new EnvelopeMetaFactory(ids.envelopeSeq(), dualTimeSource);
        meta = metaFactory.newMeta();
        repo = new InMemoryVenueOrderRepository(dualTimeSource);
        dualTimeSource = TestClocks.msTicker();
        nbbo = new NbboCache();
        nbboProvider = nbbo;
        nbboUpdater = nbbo;

        // NBBO: bid=100.00, ask=101.00
        nbbo = new NbboCache();
        listener = new CapturingListener();
        support = new DefaultVenueSupport(venueId, repo, nbboProvider, listener, ids.venueOrder(), ids.exec(), metaFactory);

        nbboUpdater.onTopOfBookUpdate(100_00L, 101_00L, dualTimeSource.nowNanos());

        // Strategy order matters: Risk → Immediate (market/crossable) → Matching Engine
        List<VenueStrategy> strategies = List.of(
                new FatFingerRiskStrategy(support, /*upPct*/0.50, /*downPct*/0.50),
                new ImmediateFillStrategy(support),
                new MatchingEngineStrategy(new MatchingEngine(support))
        );

        venue = new DefaultVenue(venueId, strategies, listener, metaFactory);
    }

    @Test
    void buyMarket_shouldAckThenFillAtAsk() {
        var cmd = NewChildCmd.builder()
                .parentId(ParentId.of("P1"))
                .childId(ChildId.of("CH-1"))
                .childClOrdId(ChildClOrdId.of("CCl-1"))
                .accountId("ACC-1")
                .domainAccountType(DomainAccountType.CUSTOMER)
                .instrumentKey(InstrumentKey.ofSymbol("ABC"))
                .side(DomainSide.BUY)
                .qty(100L)
                .ordType(DomainOrdType.MARKET)
                .tif(DomainTif.DAY)
                .venueId(venueId)       // ok to set; record carries it through
                .tsNanos(dualTimeSource.nowNanos())
                .build();

        venue.onCommand(Envelope.of(cmd, meta));

        // Verify side-effects delivered to listener, in order
        var events = listener.events();
        assertTrue(events.size() >= 2, "Expect at least ACK then FILL");
        Envelope<VenueEvent> envelope1 = events.get(0);
        Envelope<VenueEvent> envelope2 = events.get(1);
        assertInstanceOf(VenueAck.class, envelope1.payload());
        assertInstanceOf(VenueFill.class, envelope2.payload());

        var ack = (VenueAck) events.get(0).payload();
        var fill = (VenueFill) events.get(1).payload();

        assertEquals(cmd.childId(), ack.childId());
        assertEquals(cmd.childId(), fill.childId());
        assertEquals(101_00L, fill.lastPxMicros(), "BUY market lifts ask");
        assertEquals(100L, fill.lastQty());
        //assertEquals(FillSource.MATCHING_ENGINE, fill.source());

        // Repository state should reflect cumulative & leaves
        var voOpt = repo.get(cmd.childId());
        assertTrue(voOpt.isPresent(), "VO must exist");
        var vo = voOpt.get();
        assertEquals(100L, vo.cumQty());
        assertEquals(0L, vo.leavesQty());
    }

    @Test
    void sellMarket_shouldAckThenFillAtBid() {
        var cmd = NewChildCmd.builder()
                .parentId(ids.parent().allocate())
                .childId(ids.child().allocate())
                .childClOrdId(ids.childClOrd().next())
                .accountId("ACC-2")
                .domainAccountType(DomainAccountType.CUSTOMER)
                .instrumentKey(InstrumentKey.ofSymbol("ABC"))
                .side(DomainSide.SELL)
                .qty(200L)
                .ordType(DomainOrdType.MARKET)
                .tif(DomainTif.DAY)
                .venueId(venueId)
                .tsNanos(dualTimeSource.nowNanos())
                .build();

        venue.onCommand(Envelope.of(cmd, meta));

        var events = listener.events();
        // last two events should be this order's ack/fill (since listener holds all events)
        assertTrue(events.size() >= 2);
        var ack = (VenueAck) events.get(events.size() - 2).payload();
        var fill = (VenueFill) events.get(events.size() - 1).payload();

        assertEquals(cmd.childId(), ack.childId());
        assertEquals(cmd.childId(), fill.childId());
        assertEquals(100_00L, fill.lastPxMicros(), "SELL market hits bid");
        assertEquals(200L, fill.lastQty());
    }

    @Test
    void crossableLimitBuy_shouldAckThenFill_usingNbboAsk() {
        // Seed NBBO: bid = 100.00, ask = 101.00  (prices in MICROS)
        nbboUpdater.onTopOfBookUpdate(100_000_000L, 101_000_000L, dualTimeSource.nowNanos());

        // LIMIT BUY with price >= ask → crossable
        var cmd = NewChildCmd.builder()
                .parentId(ids.parent().allocate())
                .childId(ids.child().allocate())
                .childClOrdId(ids.childClOrd().next())
                .accountId("ACC-3")
                .domainAccountType(DomainAccountType.CUSTOMER)
                .instrumentKey(InstrumentKey.ofSymbol("ABC"))
                .side(DomainSide.BUY)
                .qty(50L)
                .ordType(DomainOrdType.LIMIT)
                .priceMicros(102_000_000L)   // >= 101.00 → crossable (use full micros)
                .tif(DomainTif.DAY)
                .venueId(venueId)
                .tsNanos(dualTimeSource.nowNanos())
                .build();

        venue.onCommand(Envelope.of(cmd, meta));

        var events = listener.events();

        // Find the last Ack and last Fill robustly (avoid relying on indices)
        VenueAck ack = null;
        VenueFill fill = null;
        for (int i = events.size() - 1; i >= 0 && (ack == null || fill == null); i--) {
            var e = events.get(i).payload();
            if (fill == null && e instanceof VenueFill f) fill = f;
            else if (ack == null && e instanceof VenueAck a) ack = a;
        }

        assertNotNull(ack, "Expected an Ack event");
        assertNotNull(fill, "Expected a Fill event");

        assertEquals(cmd.childId(), ack.childId());
        // CrossableLimitTopOfBookRule → execute at NBBO ask for BUY
        assertEquals(101_000_000L, fill.lastPxMicros());
        assertEquals(50L, fill.lastQty());
    }

    @Test
    void crossableLimitSell_shouldAckThenFill_usingNbboBid() {
        // Seed NBBO: bid = 100.00, ask = 101.00 (all prices in MICROS)
        nbboUpdater.onTopOfBookUpdate(100_000_000L, 101_000_000L, dualTimeSource.nowNanos());

        // LIMIT SELL with price <= bid → crossable
        var cmd = NewChildCmd.builder()
                .parentId(ids.parent().allocate())
                .childId(ids.child().allocate())
                .childClOrdId(ids.childClOrd().next())
                .accountId("ACC-4")
                .domainAccountType(DomainAccountType.CUSTOMER)
                .instrumentKey(InstrumentKey.ofSymbol("ABC"))
                .side(DomainSide.SELL)
                .qty(50L)
                .ordType(DomainOrdType.LIMIT)
                .priceMicros(99_500_000L)      // <= 100.00 → crossable
                .tif(DomainTif.DAY)
                .venueId(venueId)
                .tsNanos(dualTimeSource.nowNanos())
                .build();

        venue.onCommand(Envelope.of(cmd, meta));

        var events = listener.events();

        // Find the last Ack and last Fill without assuming exact indices
        VenueAck ack = null;
        VenueFill fill = null;
        for (int i = events.size() - 1; i >= 0 && (ack == null || fill == null); i--) {
            var e = events.get(i).payload();
            if (fill == null && e instanceof VenueFill f) fill = f;
            else if (ack == null && e instanceof VenueAck a) ack = a;
        }

        assertNotNull(ack, "Expected an Ack event");
        assertNotNull(fill, "Expected a Fill event");

        assertEquals(cmd.childId(), ack.childId());
        // CrossableLimitTopOfBookRule → SELL fills at NBBO bid
        assertEquals(100_000_000L, fill.lastPxMicros());
        assertEquals(50L, fill.lastQty());
    }

    // --- helper listener that captures all events delivered by DefaultVenue ---
    static final class CapturingListener implements VenueListener {
        private final List<Envelope<VenueEvent>> events = new CopyOnWriteArrayList<>();

        @Override
        public void onEvent(Envelope<VenueEvent> e) {
            events.add(e);
        }

        public List<Envelope<VenueEvent>> events() {
            return events;
        }
    }
}

