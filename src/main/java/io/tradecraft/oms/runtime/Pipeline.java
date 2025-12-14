// Pipeline — design: single-writer, two inbound queues
package io.tradecraft.oms.runtime;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.log.LogUtils;
import io.tradecraft.observability.trace.TraceWriter;
import io.tradecraft.oms.core.Effects;
import io.tradecraft.oms.dispatch.EffectPublisher;
import io.tradecraft.oms.dispatch.InboundDispatcher;
import io.tradecraft.oms.event.OrderEvent;

import static io.tradecraft.common.meta.Component.OMS;
import static io.tradecraft.common.meta.Flow.IN;
import static io.tradecraft.common.meta.Flow.XFORM;
import static io.tradecraft.common.meta.MessageType.EV;

public final class Pipeline implements Runnable {
    private final InboundDispatcher inboundDispatcher;
    private final EffectPublisher effectPublisher;
    private final ParentFxProcessor parentFxProcessor;
    private final TraceWriter traceWriter;
    private final EventTranslator translator;
    private final ParentFsmExecutor fsmExecutor;

    private volatile boolean running = true;

    /*
    public Pipeline(PipelineContext context) {
        this(context.inboundDispatcher(), context.effectPublisher(), context.translator(),
                context.fsmExecutor(), context.parentFxProcessor(), context.traceWriter());
    }
     */

    public Pipeline(InboundDispatcher inboundDispatcher, EffectPublisher effectPublisher,
                    EventTranslator eventTranslator, ParentFsmExecutor fsmExecutor,
                    ParentFxProcessor parentFxProcessor, TraceWriter traceWriter) {
        this.inboundDispatcher = inboundDispatcher;
        this.effectPublisher = effectPublisher;
        this.translator = eventTranslator;
        this.fsmExecutor = fsmExecutor;
        this.parentFxProcessor = parentFxProcessor;
        this.traceWriter = traceWriter;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Envelope<OrderEvent> envelope = inboundDispatcher.poll();
                if (envelope == null) {
                    continue;
                }
                LogUtils.log(OMS, EV, IN, this, envelope);
                OrderEvent translated = translator.translate(envelope, envelope.meta());
                LogUtils.log(OMS, EV, XFORM, this, translated);
                Effects effects = fsmExecutor.apply(translated, envelope.meta());
                effectPublisher.publish(effects, envelope.meta());
                parentFxProcessor.processFx(effects.parentFxes(), effects.newState());
                traceWriter.write(envelope);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                LogUtils.log(OMS, EV, IN, "Interrupted while polling; shutting down", ie);
                break;
            }
        }
    }

    // Stop the run loop cleanly (sets whatever flag run() checks).
    void stopRun() {
        // e.g., running.set(false);
        this.running = false;
    }

}
