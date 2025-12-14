package io.tradecraft.bootstrap;

import io.tradecraft.observability.trace.TraceWriter;
import io.tradecraft.oms.dispatch.EffectPublisher;
import io.tradecraft.oms.dispatch.InboundDispatcher;
import io.tradecraft.oms.runtime.EventTranslator;
import io.tradecraft.oms.runtime.ParentFsmExecutor;
import io.tradecraft.oms.runtime.ParentFxProcessor;
import io.tradecraft.oms.runtime.Pipeline;
import io.tradecraft.oms.support.ThreadGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PipelineModuleTest {

    private ThreadGuard guard;
    private InboundDispatcher inboundDispatcher;
    private EffectPublisher effectPublisher;
    private EventTranslator translator;
    private ParentFsmExecutor fsmExecutor;
    private ParentFxProcessor parentFxProcessor;
    private TraceWriter traceWriter;

    @BeforeEach
    void setup() {
        guard = mock(ThreadGuard.class);
        inboundDispatcher = mock(InboundDispatcher.class);
        effectPublisher = mock(EffectPublisher.class);
        translator = mock(EventTranslator.class);
        fsmExecutor = mock(ParentFsmExecutor.class);
        parentFxProcessor = mock(ParentFxProcessor.class);
        traceWriter = mock(TraceWriter.class);
    }

    @Test
    void testStartRunsPipelineAndBindsGuard() throws Exception {

        AtomicBoolean runCalled = new AtomicBoolean(false);

        Pipeline pipelineMock = mock(Pipeline.class);
        doAnswer(inv -> { runCalled.set(true); return null; }).when(pipelineMock).run();

        PipelineModule module = new PipelineModule(
                guard,
                "test-thread",
                () -> pipelineMock
        );

        module.start();

        Thread.sleep(50); // allow async thread to execute

        assertTrue(runCalled.get(), "Pipeline.run() should be called");

        verify(guard, times(1)).bindToCurrent();
        verify(guard, atLeastOnce()).clear();
    }

    @Test
    void testStopStopsPipelineAndInterruptsThread() throws Exception {
        Pipeline pipelineMock = mock(Pipeline.class);

        PipelineModule module = new PipelineModule(
                guard,
                "test-thread",
                inboundDispatcher,
                effectPublisher,
                translator,
                fsmExecutor,
                parentFxProcessor,
                traceWriter
        );

        // inject pipeline + thread
        var pipelineField = PipelineModule.class.getDeclaredField("pipeline");
        pipelineField.setAccessible(true);
        pipelineField.set(module, pipelineMock);

        Thread fakeThread = mock(Thread.class);
        var threadField = PipelineModule.class.getDeclaredField("thread");
        threadField.setAccessible(true);
        threadField.set(module, fakeThread);

        // --- execute stop ---
        module.stop();

        verify(pipelineMock, times(1)).stop();
        verify(fakeThread, times(1)).interrupt();
    }
}
