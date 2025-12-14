package io.tradecraft.oms.dispatch;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.oms.event.OrderEvent;

public interface InboundDispatcher {
    Envelope<OrderEvent> poll() throws InterruptedException;
}