package io.tradecraft.oms.dispatch;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;

public final class QueueManager {

    private final EventQueue<Envelope<OrderEvent>> inboundEventBus;
    private final EventQueue<Envelope<OrderEvent>> sorEventBus;

    private final EventQueue<Envelope<PubExecReport>> execReportBus;
    private final EventQueue<Envelope<PubParentIntent>> parentIntentBus;

    public QueueManager(
            EventQueue<Envelope<OrderEvent>> inboundEventBus,
            EventQueue<Envelope<OrderEvent>> sorEventBus,
            EventQueue<Envelope<PubExecReport>> execReportBus,
            EventQueue<Envelope<PubParentIntent>> parentIntentBus
    ) {
        this.inboundEventBus = inboundEventBus;
        this.sorEventBus = sorEventBus;
        this.execReportBus = execReportBus;
        this.parentIntentBus = parentIntentBus;
    }

    public InboundDispatcher inboundDispatcher() {
        return new RoundRobinInboundDispatcher(inboundEventBus, sorEventBus);
    }

    public EffectPublisher publisher(EnvelopeMetaFactory metaFactory) {
        return new DefaultEffectPublisher(execReportBus, parentIntentBus, metaFactory);
    }

    public EventQueue<Envelope<PubExecReport>> erBus() { return execReportBus; }
    public EventQueue<Envelope<PubParentIntent>> intentBus() { return parentIntentBus; }
}
