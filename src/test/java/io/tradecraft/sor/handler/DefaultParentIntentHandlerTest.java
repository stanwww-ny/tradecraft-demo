// src/test/java/io/tradecraft/sor/handler/DefaultParentIntentHandlerTest.java
package io.tradecraft.sor.handler;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.JdkQueueChannel;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.IdFactory;
import io.tradecraft.common.id.IntentId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.spi.oms.intent.ParentCancelIntent;
import io.tradecraft.common.spi.oms.intent.ParentRouteIntent;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.common.spi.sor.intent.CancelChildIntent;
import io.tradecraft.common.spi.sor.intent.NewChildIntent;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.sor.routing.DefaultVenueRouter;
import io.tradecraft.sor.routing.VenueRouter;
import io.tradecraft.venue.registry.VenueRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that DefaultParentIntentHandler converts Parent intents (from OMS) into Venue-facing child commands and
 * enqueues them onto EventQueue<PubChildIntent>.
 */
@ExtendWith(MockitoExtension.class)
class DefaultParentIntentHandlerTest {

    public static final String DEFAULT_VENUE = "DEFAULT_VENUE";
    @Mock
    EventQueue<Envelope<PubChildIntent>> childIntentBus;

    DefaultParentIntentHandler handler;

    @Captor
    ArgumentCaptor<Envelope<PubChildIntent>> childCmdCaptor;

    IdFactory ids;

    EnvelopeMetaFactory envelopeMetaFactory;
    VenueRegistry venueRegistry;
    VenueRouter venueRouter;

    Meta meta;

    @BeforeEach
    void setUp() {
        // If your DefaultParentIntentHandler has a different constructor,
        // comment out @InjectMocks and instantiate it explicitly here:
        ids = IdFactory.testIds(42L);
        childIntentBus = new JdkQueueChannel<>();
        venueRegistry = mock(VenueRegistry.class);
        when(venueRegistry.hasVenue(DEFAULT_VENUE)).thenReturn(true);
        when(venueRegistry.resolve(DEFAULT_VENUE)).thenReturn(VenueId.XNAS);
        venueRouter = new DefaultVenueRouter(venueRegistry, VenueId.XNAS);
        DualTimeSource dualTimeSource = TestClocks.msTicker();
        envelopeMetaFactory = new EnvelopeMetaFactory(ids.envelopeSeq(), dualTimeSource);
        meta = envelopeMetaFactory.newMeta();
        handler = new DefaultParentIntentHandler(childIntentBus, venueRouter, ids.child(), ids.childClOrd(), envelopeMetaFactory);
    }

    @Test
    void routeLimitIntent_isConvertedTo_NewChildIntent_andEnqueued() {
        // Arrange: build a route intent (LIMIT)
        IdFactory ids = IdFactory.testIds(42L);
        ParentId parentId = ids.parent().allocate();
        ChildId childId = ids.child().allocate();
        ChildClOrdId childClOrdId = ids.childClOrd().next();

        InstrumentKey instrument = InstrumentKey.ofSymbol("AAPL");
        IntentId intentId = IntentId.of("INTENT-1");

        ParentRouteIntent route = ParentRouteIntent.builder()
                .parentId(parentId)
                .clOrdId(ClOrdId.of("CL1"))
                .accountId("CLIENT123")
                .accountType(DomainAccountType.CUSTOMER)
                .instrumentKey(instrument)
                .side(DomainSide.BUY)
                .parentQty(1_000L)                // parentQty
                .leavesQty(1_000L)
                .ordType(DomainOrdType.LIMIT)        // ordType
                .limitPxMicros(199_500_000L)      // limitPxMicros (1,995.00)
                .tif(DomainTif.DAY)
                .expireAt(null)                   // expireAt
                .exDest(DEFAULT_VENUE)
                .targetChildQty(1_000L)           // targetChildQty
                .maxParallelChildren(1)
                .intentId(intentId)
                .tsNanos(envelopeMetaFactory.dualTimeSource().nowNanos())
                .build();

        Envelope<PubParentIntent> envelope = Envelope.of(route, meta);
        handler.onIntent(envelope);

        // Assert: a NewChildIntent is enqueued with mapped fields
        assertEquals(1, childIntentBus.size());
        PubChildIntent out = childIntentBus.poll().payload();
        assertInstanceOf(NewChildIntent.class, out, "Expected a NewChildIntent");

        NewChildIntent n = (NewChildIntent) out;
        assertEquals(parentId, n.parentId());
        assertEquals(childId, n.childId());
        assertEquals(childClOrdId, n.childClOrdId()); // adjust if your type differs
        assertEquals("CLIENT123", n.accountId());
        assertEquals(instrument, n.instrumentKey());
        assertEquals(DomainSide.BUY, n.side());
        assertEquals(1_000L, n.qty());
        assertEquals(DomainOrdType.LIMIT, n.ordType());
        assertEquals(Optional.of(199_500_000L), Optional.ofNullable(n.priceMicros()));
        assertEquals(DomainTif.DAY, n.tif());
        assertEquals(VenueId.XNAS, n.venueId());
        // If your NewChildIntent contains plan info:
        assertEquals(intentId, n.intentId());
    }

    @Test
    void routeMarketIntent_isConvertedTo_NewChildIntent_andEnqueued() {
        // Arrange: build a route intent (LIMIT)
        IdFactory ids = IdFactory.testIds(42L);
        ParentId parentId = ids.parent().allocate();
        ChildId childId = ids.child().allocate();
        ChildClOrdId childClOrdId = ids.childClOrd().next();
        IntentId intentId = ids.intent().allocate();

        InstrumentKey instrument = InstrumentKey.ofSymbol("AAPL");


        ParentRouteIntent route = ParentRouteIntent.builder()
                .parentId(parentId)
                .clOrdId(ClOrdId.of("CL1"))
                .accountId("CLIENT123")
                .accountType(DomainAccountType.CUSTOMER)
                .instrumentKey(instrument)
                .side(DomainSide.BUY)
                .parentQty(1_000L)                // parentQty
                .leavesQty(1_000L)
                .ordType(DomainOrdType.MARKET)        // ordType
                .limitPxMicros(null)      // limitPxMicros (1,995.00)
                .tif(DomainTif.DAY)
                .expireAt(null)                   // expireAt
                .exDest(DEFAULT_VENUE)
                .targetChildQty(1_000L)           // targetChildQty
                .maxParallelChildren(1)
                .intentId(intentId)
                .tsNanos(envelopeMetaFactory.dualTimeSource().nowNanos())
                .build();

        Envelope<PubParentIntent> envelope = Envelope.of(route, meta);
        handler.onIntent(envelope);

        assertEquals(1, childIntentBus.size());
        PubChildIntent out = childIntentBus.poll().payload();
        assertInstanceOf(NewChildIntent.class, out, "Expected a NewChildIntent");

        NewChildIntent n = (NewChildIntent) out;
        assertEquals(parentId, n.parentId());
        assertEquals(childId, n.childId());
        assertEquals(childClOrdId, n.childClOrdId()); // adjust if your type differs
        assertEquals("CLIENT123", n.accountId());
        assertEquals(instrument, n.instrumentKey());
        assertEquals(DomainSide.BUY, n.side());
        assertEquals(1_000L, n.qty());
        assertEquals(DomainOrdType.MARKET, n.ordType());
        assertTrue(n.priceMicros() == null || n.priceMicros() == 0);
        assertEquals(DomainTif.DAY, n.tif());
        assertEquals(VenueId.XNAS, n.venueId());
        // If your NewChildIntent contains plan info:
        // assertEquals(planId, n.planId());
    }

    void cancelIntent_isConvertedTo_CancelChildIntent_andEnqueued() {
        // Arrange: ParentCancelIntent has a Builder in your uploaded file — use it.
        IdFactory ids = IdFactory.testIds(42L);
        ParentId parentId = ids.parent().allocate();
        ChildId childId = ids.child().allocate();
        ChildClOrdId childClOrdId = ids.childClOrd().next();

        InstrumentKey instrument = InstrumentKey.ofSymbol("AAPL");
        ParentCancelIntent cancel = ParentCancelIntent.builder()
                .parentId(parentId)
                .childId(childId)
                .childClOrdId(childClOrdId)
                .instrumentKey(instrument)
                .tsNanos(envelopeMetaFactory.dualTimeSource().nowNanos())
                .build();

        // Act
        Envelope<PubParentIntent> envelope = Envelope.of(cancel, meta);
        handler.onIntent(envelope);

        // Assert
        assertEquals(1, childIntentBus.size());
        PubChildIntent out = childIntentBus.poll().payload();
        assertInstanceOf(CancelChildIntent.class, out, "Expected a CancelChildIntent");

        CancelChildIntent x = (CancelChildIntent) out;
        assertEquals(parentId, x.parentId());
        assertEquals(childId, x.childId()); // adjust getter if needed
        assertEquals(childClOrdId, x.childClOrdId());
        assertEquals(instrument, x.instrumentKey());
    }
}
