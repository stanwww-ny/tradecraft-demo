package io.tradecraft.oms.dispatch;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.utils.QueueRoundRobinPoller;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;

public final class RoundRobinInboundDispatcher implements InboundDispatcher {

    private final QueueRoundRobinPoller<Envelope<OrderEvent>> poller;

    public RoundRobinInboundDispatcher(
            EventQueue<Envelope<OrderEvent>> inboundOms,
            EventQueue<Envelope<OrderEvent>> inboundSor
    ) {
        this.poller = new QueueRoundRobinPoller<>(inboundOms, inboundSor);
    }

    @Override
    public Envelope<OrderEvent> poll() throws InterruptedException {
        return poller.poll();
    }
}
