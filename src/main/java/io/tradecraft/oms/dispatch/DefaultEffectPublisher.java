package io.tradecraft.oms.dispatch;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMeta;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.oms.core.Effects;
import io.tradecraft.oms.event.EventQueue;

public final class DefaultEffectPublisher implements EffectPublisher {

    private final EventQueue<Envelope<PubExecReport>> erBus;
    private final EventQueue<Envelope<PubParentIntent>> intentBus;
    private final EnvelopeMetaFactory metaFactory;

    public DefaultEffectPublisher(
            EventQueue<Envelope<PubExecReport>> erBus,
            EventQueue<Envelope<PubParentIntent>> intentBus,
            EnvelopeMetaFactory metaFactory
    ) {
        this.erBus = erBus;
        this.intentBus = intentBus;
        this.metaFactory = metaFactory;
    }

    @Override
    public void publish(Effects effects, Meta meta) {

        // ExecReports
        for (PubExecReport er : effects.execReports()) {

            // Clone meta for outgoing ER publication
            EnvelopeMeta pubMeta = ((EnvelopeMeta) meta)
                    .forPubEr(metaFactory.dualTimeSource().nowNanos());

            metaFactory.addHop(pubMeta, er);
            erBus.offer(Envelope.of(er, pubMeta));
        }

        // ParentIntents (use same meta)
        for (PubParentIntent pi : effects.intents()) {
            metaFactory.addHop(meta, pi);
            intentBus.offer(Envelope.of(pi, meta));
        }
    }
}
