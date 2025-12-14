package io.tradecraft.oms.runtime;

import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMeta;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.id.IdFactory;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.observability.trace.TraceWriter;
import io.tradecraft.oms.core.Effects;
import io.tradecraft.oms.core.OrderState;
import io.tradecraft.oms.dispatch.EffectPublisher;
import io.tradecraft.oms.dispatch.InboundDispatcher;
import io.tradecraft.oms.event.OrderEvent;
import io.tradecraft.util.sample.OrderEventSamples;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineTest {
    IdFactory ids;
    EnvelopeMetaFactory envelopeMetaFactory;
    Meta meta;

    static {
        LoggerFactory.getLogger("init");
    }

    @BeforeEach
    void setup() {
        ids = IdFactory.testIds(42L);
        DualTimeSource dualTimeSource = TestClocks.msTicker();
        envelopeMetaFactory = new EnvelopeMetaFactory(ids.envelopeSeq(), dualTimeSource);
        meta = envelopeMetaFactory.newMeta();
    }
    @Test
    void testPipelineProcessesOneEventAndStops() throws Exception {
        // ================
        // 1. Mock components
        // ================
        InboundDispatcher inbound = mock(InboundDispatcher.class);
        ParentFsmExecutor fsmExecutor = mock(ParentFsmExecutor.class);
        ParentFxProcessor fxProcessor = mock(ParentFxProcessor.class);
        EffectPublisher publisher = mock(EffectPublisher.class);
        EventTranslator translator = mock(EventTranslator.class);
        TraceWriter traceWriter = mock(TraceWriter.class);

        // ================
        // 2. Prepare a fake event
        // ================
        OrderEvent evBoundParentNew = OrderEventSamples.evBoundParentNew();
        OrderEvent evNew = OrderEventSamples.evNew();
        EnvelopeMeta meta = (EnvelopeMeta) this.meta;
        Envelope<OrderEvent> envelope = Envelope.of(evBoundParentNew, meta);
        when(inbound.poll()).thenReturn(envelope)  // 1st poll returns event
                .thenReturn(null);
        when(translator.translate(envelope,meta)).thenReturn(evNew);

        OrderState orderState = mock(OrderState.class);
        Effects fakeEffects = Effects.withState(orderState).build();
        when(fsmExecutor.apply(evNew, meta)).thenReturn(fakeEffects);

        // ================
        // 3. Build pipeline
        // ================
        Pipeline pipeline = new Pipeline(
                inbound,
                publisher,
                translator,
                fsmExecutor,
                fxProcessor,
                traceWriter
        );

        // ================
        // 4. Run pipeline in background thread
        // ================
        Thread t = new Thread(() -> {
            pipeline.run();
        });

        t.start();

        // Let 1 cycle run
        Thread.sleep(50);

        // Stop the pipeline
        pipeline.stopRun();

        // Wait for clean exit
        t.join(200);

        assertFalse(t.isAlive(), "Pipeline thread should stop after stopRun()");

        // ================
        // 5. Verify behavior
        // ================

        verify(inbound, atLeastOnce()).poll();
        verify(translator, times(1)).translate(envelope, meta);
        verify(fsmExecutor, times(1)).apply(evNew, meta);
        verify(fxProcessor, times(1)).processFx(fakeEffects.parentFxes(), orderState);
        verify(publisher, times(1)).publish(fakeEffects, meta);
        verify(traceWriter, times(1)).write(envelope);

        InOrder inOrder = inOrder(translator, fsmExecutor, publisher, fxProcessor, traceWriter);

        inOrder.verify(translator).translate(envelope, meta);
        inOrder.verify(fsmExecutor).apply(evNew, meta);
        inOrder.verify(publisher).publish(fakeEffects, meta);
        inOrder.verify(fxProcessor).processFx(fakeEffects.parentFxes(), orderState);
        inOrder.verify(traceWriter).write(envelope);

        System.out.println("✔ PipelineTest passed.");
    }

    @Test
    void testNullEnvelopeDoesNothing() throws Exception {
        // ================
        // 1. Mock components
        // ================
        InboundDispatcher inbound = mock(InboundDispatcher.class);
        ParentFsmExecutor fsmExecutor = mock(ParentFsmExecutor.class);
        ParentFxProcessor fxProcessor = mock(ParentFxProcessor.class);
        EffectPublisher publisher = mock(EffectPublisher.class);
        EventTranslator translator = mock(EventTranslator.class);
        TraceWriter traceWriter = mock(TraceWriter.class);

        // ================
        // 2. Prepare a fake event
        // ================
        when(inbound.poll()).thenReturn(null)  // 1st poll returns event
                .thenReturn(null);

        // ================
        // 3. Build pipeline
        // ================
        Pipeline pipeline = new Pipeline(
                inbound,
                publisher,
                translator,
                fsmExecutor,
                fxProcessor,
                traceWriter
        );

        // ================
        // 4. Run pipeline in background thread
        // ================
        Thread t = new Thread(() -> {
            pipeline.run();
        });

        t.start();

        // Let 1 cycle run
        Thread.sleep(50);

        // Stop the pipeline
        pipeline.stopRun();

        // Wait for clean exit
        t.join(200);

        assertFalse(t.isAlive(), "Pipeline thread should stop after stopRun()");

        // ================
        // 5. Verify behavior
        // ================
        verify(inbound, atLeastOnce()).poll();
        verify(fsmExecutor, times(0)).apply(any(), any());
        verify(fxProcessor, times(0)).processFx(any(), any());
        verify(publisher, times(0)).publish(any(), any());
        verify(traceWriter, times(0)).write(any());

        System.out.println("✔ PipelineTest passed.");
    }

    @Test
    void testPipelineStopsOnInterruptedException() throws Exception {
        InboundDispatcher inbound = mock(InboundDispatcher.class);
        ParentFsmExecutor fsmExecutor = mock(ParentFsmExecutor.class);
        ParentFxProcessor fxProcessor = mock(ParentFxProcessor.class);
        EffectPublisher publisher = mock(EffectPublisher.class);
        EventTranslator translator = mock(EventTranslator.class);
        TraceWriter traceWriter = mock(TraceWriter.class);

        when(inbound.poll())
                .thenThrow(new InterruptedException("forced"))
                .thenReturn(null);

        Pipeline pipeline = new Pipeline(inbound, publisher, translator, fsmExecutor, fxProcessor, traceWriter);

        Thread t = new Thread(pipeline::run);
        t.start();

        t.join(200);

        assertFalse(t.isAlive(), "Pipeline should stop after InterruptedException");
    }
}
