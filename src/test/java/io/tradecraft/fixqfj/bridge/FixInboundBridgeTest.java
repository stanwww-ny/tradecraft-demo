package io.tradecraft.fixqfj.bridge;

// -----------------------------------------------------------------------------
// Test Suite: io.tradecraft — OMS → SOR → Venue MVP-1
// JUnit 5 + Mockito (inline) + AssertJ (optional) skeletons.
// Drop these under securityIdSource/test/java, keep package names consistent.
// -----------------------------------------------------------------------------

// ===============================
// 1) FixInboundBridgeTest
// ===============================

import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.fixqfj.event.FixEvCancelReq;
import io.tradecraft.fixqfj.event.FixEvInbound;
import io.tradecraft.fixqfj.event.FixEvParentNew;
import io.tradecraft.fixqfj.event.FixEvReplaceReq;
import io.tradecraft.fixqfj.session.SessionKey;
import io.tradecraft.oms.event.EvBoundCancelReq;
import io.tradecraft.oms.event.EvBoundParentNew;
import io.tradecraft.oms.event.EvBoundReplaceReq;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;
import io.tradecraft.util.sample.AccountSamples;
import io.tradecraft.util.sample.ClOrdIdSamples;
import io.tradecraft.util.sample.ExDestSamples;
import io.tradecraft.util.sample.InstrumentKeySamples;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for FixInboundQueueAdapter → OrderEvent translation.
 * <p>
 * NOTE: This test assumes FixInboundQueueAdapter exposes a method that accepts FixEvInbound (e.g.,
 * offer()/accept()/publish()/onMessage()). Adjust the call-site name as needed. If translation is private, the test
 * observes the EventQueue<OrderEvent> side effect.
 */
@Disabled("Waiting for FIX simulator to be stable")
public class FixInboundBridgeTest {

    private EventQueue<OrderEvent> omsQueue;   // capture translated OMS events

    private FixInboundBridge adapter;

    // --- helper accepting different possible public method names without coupling ---
    private static void publish(FixInboundBridge a, FixEvInbound e) {
        a.offer(e);
    }

    @BeforeEach
    void setUp() {
        omsQueue = mock(EventQueue.class);
        adapter = new FixInboundBridge(
                omsQueue
        );
    }

    @Test
    void newOrderSingle_isTranslatedTo_EvNew_withAllKeyFields() {
        // Arrange: synthetic FIX-inbound parent-new event (already parsed by your FIX layer)
        FixEvParentNew fixNew = new FixEvParentNew(
                /* session */ new SessionKey("FIX.4.4", "TRADER", "OMS", null),
                /* clOrdId */ ClOrdIdSamples.CL_ORD_ID_001,
                /* account */ AccountSamples.ACC1,
                AccountSamples.ACC1_TYPE,
                /* instrumentKey  */ InstrumentKeySamples.AAPL,
                /* side    */ DomainSide.BUY,                  // 1=Buy, 2=Sell (FIX)
                /* qty     */ 1_000L,
                /* ordType */ DomainOrdType.LIMIT,                  // 2=Limit
                /* limitPxMicros*/ 195_250_000L,        // $195.25 → micros
                /* tif */ DomainTif.DAY,              // Day
                /* exDest  */ ExDestSamples.XNYS,
                /* tsNanos*/ 111_222_333L
        );

        // Stubbing any mapping behavior that the adapter consults (if any)

        // Act: push the inbound event into the adapter
        publish(adapter, fixNew);

        // Assert: adapter enqueued an EvNew to the OMS queue with expected fields
        ArgumentCaptor<OrderEvent> evCap = ArgumentCaptor.forClass(OrderEvent.class);
        verify(omsQueue, times(1)).offer(evCap.capture());

        OrderEvent ev = evCap.getValue();
        assertInstanceOf(EvBoundParentNew.class, ev, "Should publish EvNew");
        EvBoundParentNew n = (EvBoundParentNew) ev;
        assertEquals(ClOrdIdSamples.CL_ORD_ID_001, n.clOrdId());
        assertEquals(InstrumentKeySamples.AAPL, n.instrumentKey());
        assertEquals(1_000L, n.qty());
        assertEquals(195_250_000L, n.limitPxMicros());
        assertEquals(DomainSide.BUY, n.side());
        assertEquals(AccountSamples.ACC1, n.accountId());
        assertEquals(ExDestSamples.XNYS, n.exDest());
        assertNull(n.parentId());                        // PI should be derived from clOrdId/session
    }

    @Test
    void newOrderSingle_marketOrder_setsLimitPxNull_DAY() {
        FixEvParentNew fixNew = new FixEvParentNew(
                /* session   */ new SessionKey("FIX.4.4", "TRADER", "OMS", null),
                /* clOrdId   */ ClOrdIdSamples.CL_ORD_ID_001,
                AccountSamples.ACC1,
                AccountSamples.ACC1_TYPE,
                InstrumentKeySamples.AAPL,
                DomainSide.BUY,
                /* qty       */ 1_000L,
                /* ordType   */ DomainOrdType.MARKET,
                /* limitPx   */ 123_450_000L,     // should be ignored for market
                /* tif       */ DomainTif.DAY,
                ExDestSamples.XNYS,
                /* tsNanos   */ 666_777_888L
        );

        publish(adapter, fixNew);

        ArgumentCaptor<OrderEvent> evCap = ArgumentCaptor.forClass(OrderEvent.class);
        verify(omsQueue).offer(evCap.capture());

        EvBoundParentNew e = (EvBoundParentNew) evCap.getValue();

        assertEquals(DomainOrdType.MARKET, e.ordType());
        assertEquals(0, e.limitPxMicros(), "Market NOS must not carry a limit price");
        assertEquals(DomainTif.DAY, e.tif());
        assertEquals(1_000L, e.qty());
        assertEquals(DomainSide.BUY, e.side());
        assertEquals(InstrumentKeySamples.AAPL, e.instrumentKey());
        assertEquals(ExDestSamples.XNYS, e.exDest());
        assertNull(e.parentId());
    }

    @Test
    void newOrderSingle_marketIOC_setsLimitPxNull_andTifIOC() {
        FixEvParentNew fixNew = new FixEvParentNew(
                /* session   */ new SessionKey("FIX.4.4", "TRADER", "OMS", null),
                /* clOrdId   */ ClOrdIdSamples.CL_ORD_ID_001,
                AccountSamples.ACC1,
                AccountSamples.ACC1_TYPE,
                InstrumentKeySamples.AAPL,
                DomainSide.SELL,
                /* qty       */ 2_500L,
                /* ordType   */ DomainOrdType.MARKET,
                /* limitPx   */ 999_990_000L,     // should be ignored for market
                /* tif       */ DomainTif.IOC,
                ExDestSamples.XNYS,
                /* tsNanos   */ 777_888_999L
        );

        publish(adapter, fixNew);

        ArgumentCaptor<OrderEvent> evCap = ArgumentCaptor.forClass(OrderEvent.class);
        verify(omsQueue).offer(evCap.capture());

        EvBoundParentNew e = (EvBoundParentNew) evCap.getValue();

        assertEquals(DomainOrdType.MARKET, e.ordType());
        assertEquals(0, e.limitPxMicros(), "Market NOS must not carry a limit price");
        assertEquals(DomainTif.IOC, e.tif());
        assertEquals(2_500L, e.qty());
        assertEquals(DomainSide.SELL, e.side());
        assertEquals(InstrumentKeySamples.AAPL, e.instrumentKey());
        assertEquals(ExDestSamples.XNYS, e.exDest());
        assertNull(e.parentId());
    }


    @Test
    void cancelRequest_isTranslatedTo_EvCancelReq_usingOrigClOrdId_whenPresent() {
        // Arrange
        FixEvCancelReq fixCancel = new FixEvCancelReq(
                /* session */ new SessionKey("FIX.4.4", "TRADER", "OMS", null),
                /* clOrdId     */ ClOrdIdSamples.CL_ORD_ID_999,
                /* origClOrdId */  ClOrdIdSamples.CL_ORD_ID_001,
                AccountSamples.ACC1,
                AccountSamples.ACC1_TYPE,
                InstrumentKeySamples.AAPL,
                DomainSide.BUY,
                1000L,
                /* tsNanos     */ 222_333_444L
        );

        // Act
        publish(adapter, fixCancel);

        // Assert
        ArgumentCaptor<OrderEvent> evCap = ArgumentCaptor.forClass(OrderEvent.class);
        verify(omsQueue, times(1)).offer(evCap.capture());

        OrderEvent ev = evCap.getValue();
        assertInstanceOf(EvBoundCancelReq.class, ev, "Should publish EvCancelReq");
        EvBoundCancelReq c = (EvBoundCancelReq) ev;
        assertEquals(ClOrdIdSamples.CL_ORD_ID_999, c.clOrdId(), "origClOrdId should map to ParentOrderId");
        assertEquals(ClOrdIdSamples.CL_ORD_ID_001, c.origClOrdId(), "origClOrdId should map to ParentOrderId");
        assertNull(c.parentId());
    }

    @Test
    void cancelRequest_fallsBackTo_ClOrdId_when_OrigMissing() {
        FixEvCancelReq fixCancel = new FixEvCancelReq(
                /* session */ new SessionKey("FIX.4.4", "TRADER", "OMS", null),
                /* clOrdId     */ ClOrdIdSamples.CL_ORD_ID_555,
                /* origClOrdId */ null,
                AccountSamples.ACC1,
                AccountSamples.ACC1_TYPE,
                InstrumentKeySamples.AAPL,
                DomainSide.BUY,
                1000L,
                /* tsNanos     */ 222_333_444L
        );

        publish(adapter, fixCancel);

        ArgumentCaptor<OrderEvent> evCap = ArgumentCaptor.forClass(OrderEvent.class);
        verify(omsQueue).offer(evCap.capture());

        EvBoundCancelReq c = (EvBoundCancelReq) evCap.getValue();
        assertEquals(ClOrdIdSamples.CL_ORD_ID_555, c.clOrdId());
    }

    @Test
    void replaceRequest_fallsBackTo_ClOrdId_when_OrigMissing() {
        FixEvReplaceReq fixReplace = new FixEvReplaceReq(
                /* session     */ new SessionKey("FIX.4.4", "TRADER", "OMS", null),
                /* clOrdId     */ ClOrdIdSamples.CL_ORD_ID_555,
                /* origClOrdId */ null,
                AccountSamples.ACC1,
                AccountSamples.ACC1_TYPE,
                InstrumentKeySamples.AAPL,
                DomainSide.BUY,
                /* qty         */ 1_200L,
                /* ordType     */ DomainOrdType.LIMIT,
                /* limitPx     */ 185_12_000L,          // example: $185.12 in micros
                /* tif         */ DomainTif.DAY,
                ExDestSamples.XNYS,
                /* tsNanos     */ 333_444_555L
        );

        publish(adapter, fixReplace);

        ArgumentCaptor<OrderEvent> evCap = ArgumentCaptor.forClass(OrderEvent.class);
        verify(omsQueue).offer(evCap.capture());

        EvBoundReplaceReq r = (EvBoundReplaceReq) evCap.getValue();

        // Fallback behavior: when OrigClOrdId missing, use ClOrdId as the target reference too.
        assertEquals(ClOrdIdSamples.CL_ORD_ID_555, r.clOrdId());
        assertEquals(ClOrdIdSamples.CL_ORD_ID_555, r.origClOrdId());

        // Sanity: other fields should be passed through unchanged
        assertEquals(AccountSamples.ACC1, r.accountId());
        assertEquals(AccountSamples.ACC1_TYPE, r.domainAccountType());
        assertEquals(InstrumentKeySamples.AAPL, r.instrumentKey());
        assertEquals(DomainSide.BUY, r.side());
        assertEquals(1_200L, r.qty());
        assertEquals(DomainOrdType.LIMIT, r.ordType());
        assertEquals(Long.valueOf(185_12_000L), r.limitPxMicros());
        assertEquals(DomainTif.DAY, r.tif());
    }

    @Test
    void replaceRequest_uses_OrigClOrdId_when_Present() {
        FixEvReplaceReq fixReplace = new FixEvReplaceReq(
                /* session     */ new SessionKey("FIX.4.4", "TRADER", "OMS", null),
                /* clOrdId     */ ClOrdIdSamples.CL_ORD_ID_999,
                /* origClOrdId */ ClOrdIdSamples.CL_ORD_ID_555,
                AccountSamples.ACC1,
                AccountSamples.ACC1_TYPE,
                InstrumentKeySamples.AAPL,
                DomainSide.BUY,
                /* qty         */ 1_500L,
                /* ordType     */ DomainOrdType.MARKET,
                /* limitPx     */ null,                 // MARKET has no limit
                /* tif         */ DomainTif.IOC,
                ExDestSamples.XNYS,
                /* tsNanos     */ 444_555_666L
        );

        publish(adapter, fixReplace);

        ArgumentCaptor<OrderEvent> evCap = ArgumentCaptor.forClass(OrderEvent.class);
        verify(omsQueue).offer(evCap.capture());

        EvBoundReplaceReq r = (EvBoundReplaceReq) evCap.getValue();

        // When provided, OrigClOrdId should be preserved.
        assertEquals(ClOrdIdSamples.CL_ORD_ID_999, r.clOrdId());
        assertEquals(ClOrdIdSamples.CL_ORD_ID_555, r.origClOrdId());

        // Sanity: payload
        assertEquals(AccountSamples.ACC1, r.accountId());
        assertEquals(AccountSamples.ACC1_TYPE, r.domainAccountType());
        assertEquals(InstrumentKeySamples.AAPL, r.instrumentKey());
        assertEquals(DomainSide.BUY, r.side());
        assertEquals(1_500L, r.qty());
        assertEquals(DomainOrdType.MARKET, r.ordType());
        assertNull(r.limitPxMicros());
        assertEquals(DomainTif.IOC, r.tif());
        assertEquals(ExDestSamples.XNYS, r.exDest());
    }

}



