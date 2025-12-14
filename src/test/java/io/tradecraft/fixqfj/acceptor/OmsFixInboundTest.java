package io.tradecraft.fixqfj.acceptor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.JdkQueueChannel;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.IdFactory;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.fixqfj.event.FixEvParentNew;
import io.tradecraft.fixqfj.session.SessionIndex;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import quickfix.SessionID;
import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReplaceRequest;
import quickfix.fix44.OrderCancelRequest;

import static io.tradecraft.util.sample.TradeSamples.NOS_BUY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class OmsFixInboundTest {

    OmsFixInbound omsFixInbound;
    SessionID sid;
    SessionID sidReverse;
    EventQueue<Envelope<OrderEvent>> eventQueue;
    MeterRegistry meterRegistry;
    SessionIndex sessionIndex;
    EnvelopeMetaFactory envelopeMetaFactory;
    IdFactory idFactory;
    DualTimeSource timeSource;
    long tick1, tick2, tick3;
    @Mock
    DualTimeSource dualTimeSource;
    static final long NOW_NANOS = 123_456_789L;

    static FixEvParentNew nosEvent(ClOrdId clOrdId) {
        // SessionKey is unknown here; just mock it (non-final) or pass a dummy if it’s a simple value object.
        var sessionKey = mock(SessionKey.class);
        return new FixEvParentNew(
                sessionKey,
                clOrdId,
                AccountSamples.ACC1,
                AccountSamples.ACC1_TYPE,
                InstrumentKeySamples.AAPL,
                DomainSide.BUY,
                100L,
                DomainOrdType.LIMIT,         // Limit
                123_450_000L, // 123.45 in micros
                DomainTif.DAY,
                ExDestSamples.XNYS,
                42L
        );
    }

    @BeforeEach
    void setup() {
        idFactory = IdFactory.testIds(42L);
        timeSource = TestClocks.msTicker();
        tick1 = timeSource.nowNanos();
        tick2 = timeSource.nowNanos();
        tick3 = timeSource.nowNanos();
        timeSource = TestClocks.msTicker();
        envelopeMetaFactory = new EnvelopeMetaFactory(idFactory.envelopeSeq(), timeSource);
        sid = new SessionID("FIX.4.4", "TRADER", "OMS");
        sidReverse = new SessionID("FIX.4.4", "OMS", "TRADER");
        meterRegistry = new SimpleMeterRegistry();
        sessionIndex = mock(SessionIndex.class);

        eventQueue = new JdkQueueChannel<>();
        omsFixInbound = new OmsFixInbound(eventQueue, envelopeMetaFactory, sessionIndex, meterRegistry);
    }

    @Test
    @Disabled("Temporarily skipping due to flaky behavior in CI")
    void onLogon_putsSessionInIndex() {
        SessionKey key = mock(SessionKey.class);

        // mock static factory
        try (MockedStatic<SessionKey> staticMock = mockStatic(SessionKey.class)) {
            staticMock.when(() -> SessionKey.of(sid)).thenReturn(key);

            omsFixInbound.onLogon(sid);
        }
    }

    @Test
    void testFromAppWithNewOrderSingle() throws Exception {
        omsFixInbound.fromApp(NOS_BUY, sid);
        Assertions.assertEquals(1, eventQueue.size());
        Assertions.assertInstanceOf(EvBoundParentNew.class, eventQueue.poll().payload());
    }

    /*
    @Test
    void fromApp_NewOrderSingle_enqueues_EvBoundParentNew() throws Exception {
        // Build a minimal, valid FIX.4.4 NOS
        NewOrderSingle msg = new NewOrderSingle(
                new ClOrdID("C-NOS-1"),
                new Side(Side.BUY),
                new TransactTime(new java.util.Date()),
                new OrdType(OrdType.LIMIT)
        );
        msg.set(new Symbol("AAPL"));
        msg.set(new OrderQty(100));
        msg.set(new Price(123.45));
        msg.set(new TimeInForce(TimeInForce.DAY));
        msg.set(new Account("ACC1"));
        msg.set(new ExDestination("XNYS"));

        ArgumentCaptor<OrderEvent> cap = ArgumentCaptor.forClass(OrderEvent.class);

        inboundApp.fromApp(msg, sid);

        verify(eventQueue, times(1)).offer(cap.capture());
        assertThat(cap.getValue()).isInstanceOf(EvBoundParentNew.class);
    }
    @Test
    void testFromAppOnMessageFails() throws Exception {
        NewOrderSingle msg = new NewOrderSingle();
        FixEvParentNew ev = nosEvent(ClOrdIdSamples.CL_ORD_ID_001);

        try (MockedStatic<FixInboundMapper> mapper = mockStatic(FixInboundMapper.class)) {
            mapper.when(() -> FixInboundMapper.mapNos(any(), any(), anyLong())).thenReturn(ev);
            when(inbound.offer(ev)).thenReturn(false);

            omsFixInbound.fromApp(msg, sid);

            verify(inbound).offer(ev);
            // Verify drops counter incremented
        }
    }

    @Test
    void testFromAppThrowsException() throws Exception {
        NewOrderSingle msg = new NewOrderSingle();

        try (MockedStatic<FixInboundMapper> mapper = mockStatic(FixInboundMapper.class)) {
            mapper.when(() -> FixInboundMapper.mapNos(any(), any(), anyLong()))
                    .thenThrow(new FieldNotFound(11)); // ClOrdID missing
            assertThrows(RuntimeException.class, () -> omsFixInbound.fromApp(msg, sid));
        }
    }

     */

    @Test
    void mapNos_limit_translates_all_core_fields() throws Exception {
        var nos = new NewOrderSingle(
                new ClOrdID(ClOrdIdSamples.CL_ORD_ID_001.value()),
                new Side(Side.BUY),
                new TransactTime(),
                new OrdType(OrdType.LIMIT)
        );
        nos.set(new Symbol(InstrumentKeySamples.AAPL.symbol()));
        nos.set(new OrderQty(1000));
        nos.set(new Price(185.43));
        nos.set(new TimeInForce(TimeInForce.DAY));
        nos.setString(100, ExDestSamples.XNYS); // ExDestination (tag 100)

        omsFixInbound.onMessage(nos, sid);
        EvBoundParentNew out = (EvBoundParentNew) eventQueue.poll().payload();
        assertNotNull(out);
        assertEquals(SessionKey.of(sidReverse), out.sessionKey());
        assertEquals(ClOrdIdSamples.CL_ORD_ID_001, out.clOrdId());
        assertEquals(InstrumentKeySamples.AAPL.symbol(), out.instrumentKey().symbol());
        assertEquals(DomainSide.BUY, out.side());
        assertEquals(1000L, out.qty());
        assertEquals(Math.round(185.43 * 1_000_000d), out.limitPxMicros());
        assertEquals(DomainOrdType.LIMIT, out.ordType());
        assertEquals(DomainTif.DAY, out.tif());
        assertEquals(ExDestSamples.XNYS, out.exDest());
        assertEquals(tick2, out.tsNanos());
    }

    @Test
    void mapNos_market_sets_priceMicros_to_zero() throws Exception {
        var nos = new NewOrderSingle(
                new ClOrdID(ClOrdIdSamples.CL_ORD_ID_999.value()),
                new Side(Side.SELL),
                new TransactTime(),
                new OrdType(OrdType.MARKET)
        );
        nos.set(new Symbol(InstrumentKeySamples.MSFT.symbol()));
        nos.set(new OrderQty(250));
        nos.set(new TimeInForce(TimeInForce.IMMEDIATE_OR_CANCEL));
        nos.setString(100, ExDestSamples.XNYS);

        omsFixInbound.onMessage(nos, sid);
        EvBoundParentNew out = (EvBoundParentNew) eventQueue.poll().payload();

        assertEquals(DomainOrdType.MARKET, out.ordType());
        assertEquals(0L, out.limitPxMicros(), "Market should carry 0 micros by convention");
        assertEquals(DomainTif.IOC, out.tif());
        assertEquals(ExDestSamples.XNYS, out.exDest());
        assertEquals(250L, out.qty());
        assertEquals(DomainSide.SELL, out.side());
    }

    @Test
    void mapCancel_translates_origClOrdId_and_quantity_optional() throws Exception {
        var cancel = new OrderCancelRequest(
                new OrigClOrdID(ClOrdIdSamples.CL_ORD_ID_001.value()),
                new ClOrdID(ClOrdIdSamples.CL_ORD_ID_999.value()),
                new Side(Side.BUY),
                new TransactTime()
        );
        cancel.set(new Symbol(InstrumentKeySamples.AAPL.symbol()));
        // qty optional for some flows; omit here intentionally

        omsFixInbound.onMessage(cancel, sid);
        EvBoundCancelReq out = (EvBoundCancelReq) eventQueue.poll().payload();

        assertEquals(SessionKey.of(sidReverse), out.sessionKey());
        assertEquals(ClOrdIdSamples.CL_ORD_ID_001, out.origClOrdId());
        assertEquals(ClOrdIdSamples.CL_ORD_ID_999, out.clOrdId());
        assertEquals(InstrumentKeySamples.AAPL.symbol(), out.instrumentKey().symbol());
        assertEquals(DomainSide.BUY, out.side());
    }

    @Test
    void mapReplace_translates_new_values_and_micros() throws Exception {
        var replace = new OrderCancelReplaceRequest(
                new OrigClOrdID(ClOrdIdSamples.CL_ORD_ID_001.value()),
                new ClOrdID(ClOrdIdSamples.CL_ORD_ID_999.value()),
                new Side(Side.BUY),
                new TransactTime(),
                new OrdType(OrdType.LIMIT)
        );
        replace.set(new Symbol(InstrumentKeySamples.AAPL.symbol()));
        replace.set(new OrderQty(1500));
        replace.set(new Price(186.01));
        replace.set(new TimeInForce(TimeInForce.DAY));

        omsFixInbound.onMessage(replace, sid);
        EvBoundReplaceReq out = (EvBoundReplaceReq) eventQueue.poll().payload();

        assertEquals(SessionKey.of(sidReverse), out.sessionKey());
        assertEquals(ClOrdIdSamples.CL_ORD_ID_001, out.origClOrdId());
        assertEquals(ClOrdIdSamples.CL_ORD_ID_999, out.clOrdId());
        assertEquals(1500L, out.qty());
        assertEquals(Math.round(186.01 * 1_000_000d), out.limitPxMicros());
        assertEquals(DomainTif.DAY, out.tif());
    }

}
