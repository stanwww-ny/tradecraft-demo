package io.tradecraft.fixqfj.bridge;

import io.tradecraft.fixqfj.event.FixEvInbound;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;

/**
 * Bridges legacy FIX-inbound EvInbound queue to the new Pipeline fixBus of OrderEvent. Only offer(...) is used by
 * OmsFixServer/OmsFixInbound. poll()/size() are vestigial.
 */
public final class FixInboundBridge implements InboundBridge {
    private final EventQueue<OrderEvent> inboundBus;

    public FixInboundBridge(EventQueue<OrderEvent> inboundBus) {
        this.inboundBus = inboundBus;
    }

    @Override
    public boolean offer(FixEvInbound ev) {
        OrderEvent mapped = null;
        return inboundBus.offer(mapped);
    }

}
