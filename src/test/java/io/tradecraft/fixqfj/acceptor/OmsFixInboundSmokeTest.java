package io.tradecraft.fixqfj.acceptor;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.id.IdFactory;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.fixqfj.session.SessionIndex;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;
import io.tradecraft.common.envelope.JdkQueueChannel;
import org.junit.jupiter.api.Test;
import quickfix.SessionID;
import quickfix.fix44.NewOrderSingle;
import quickfix.field.*;

import static org.junit.jupiter.api.Assertions.*;

class OmsFixInboundSmokeTest {

    @Test
    void inbound_accepts_new_order_and_emits_event() throws Exception {
        // --- Arrange ---
        EventQueue<Envelope<OrderEvent>> inboundBus = new JdkQueueChannel<>();
        IdFactory ids = IdFactory.testIds(42L);
        DualTimeSource dualTimeSource = TestClocks.msTicker();
        EnvelopeMetaFactory metaFactory = new EnvelopeMetaFactory(ids.envelopeSeq(), dualTimeSource);
        SessionIndex sessionIndex = new SessionIndex();
        var meters = new SimpleMeterRegistry();

        OmsFixInbound inbound = new OmsFixInbound(
                inboundBus,
                metaFactory,
                sessionIndex,
                meters
        );

        SessionID sid = new SessionID("FIX.4.4", "CLIENT", "OMS");

        NewOrderSingle nos = new NewOrderSingle(
                new ClOrdID("CO-0001"),
                new Side(Side.BUY),
                new TransactTime(),
                new OrdType(OrdType.MARKET)
        );
        nos.set(new OrderQty(100));
        nos.set(new Symbol("AAPL"));

        // --- Act ---
        inbound.fromApp(nos, sid);

        // --- Assert ---
        Envelope<OrderEvent> env = inboundBus.poll();
        assertNotNull(env, "Expected inbound event to be published");
        assertNotNull(env.payload(), "Event payload must not be null");
        assertNotNull(env.meta(), "Envelope meta must not be null");
    }
}
