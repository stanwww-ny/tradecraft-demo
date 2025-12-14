// securityIdSource/test/java/org/example/v4/oms/fixin/mapper/FixInboundMapperTest.java
package io.tradecraft.fixqfj.mapper;

import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.fixqfj.event.FixEvCancelReq;
import io.tradecraft.fixqfj.event.FixEvParentNew;
import io.tradecraft.fixqfj.event.FixEvReplaceReq;
import io.tradecraft.fixqfj.session.SessionKey;
import io.tradecraft.util.sample.ClOrdIdSamples;
import io.tradecraft.util.sample.ExDestSamples;
import io.tradecraft.util.sample.InstrumentKeySamples;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class FixInboundMapperTest {

    private static SessionID sid() {
        return new SessionID("FIX.4.4", "TRADER", "OMS");
    }

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

        long nowNanos = 42L;
        FixEvParentNew out = FixInboundMapper.mapNos(nos, sid(), nowNanos);

        assertNotNull(out);
        assertEquals(SessionKey.of(sid()), out.sessionKey());
        assertEquals(ClOrdIdSamples.CL_ORD_ID_001, out.clOrdId());
        assertEquals(InstrumentKeySamples.AAPL.symbol(), out.instrumentKey().symbol());
        assertEquals(DomainSide.BUY, out.side());
        assertEquals(1000L, out.qty());
        assertEquals(Math.round(185.43 * 1_000_000d), out.limitPxMicros());
        assertEquals(DomainOrdType.LIMIT, out.ordType());
        assertEquals(DomainTif.DAY, out.tif());
        assertEquals(ExDestSamples.XNYS, out.exDest());
        assertEquals(nowNanos, out.ingressNanos());
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

        FixEvParentNew out = FixInboundMapper.mapNos(nos, sid(), 777L);

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

        FixEvCancelReq out = FixInboundMapper.mapCancel(cancel, sid(), 123L);

        assertEquals(SessionKey.of(sid()), out.sessionKey());
        assertEquals(ClOrdIdSamples.CL_ORD_ID_001, out.origClOrdId());
        assertEquals(ClOrdIdSamples.CL_ORD_ID_999, out.clOrdId());
        assertEquals(InstrumentKeySamples.AAPL.symbol(), out.instrumentKey().symbol());
        assertEquals(DomainSide.BUY, out.side());
    }

    @Test
    void mapReplace_translates_new_values_and_micros() throws Exception {
        var rep = new OrderCancelReplaceRequest(
                new OrigClOrdID(ClOrdIdSamples.CL_ORD_ID_001.value()),
                new ClOrdID(ClOrdIdSamples.CL_ORD_ID_999.value()),
                new Side(Side.BUY),
                new TransactTime(),
                new OrdType(OrdType.LIMIT)
        );
        rep.set(new Symbol(InstrumentKeySamples.AAPL.symbol()));
        rep.set(new OrderQty(1500));
        rep.set(new Price(186.01));
        rep.set(new TimeInForce(TimeInForce.DAY));

        FixEvReplaceReq out = FixInboundMapper.mapReplace(rep, sid(), 456L);

        assertEquals(ClOrdIdSamples.CL_ORD_ID_001, out.origClOrdId());
        assertEquals(ClOrdIdSamples.CL_ORD_ID_999, out.clOrdId());
        assertEquals(1500L, out.qty());
        assertEquals(Math.round(186.01 * 1_000_000d), out.limitPxMicros());
        assertEquals(DomainTif.DAY, out.tif());
    }
}
