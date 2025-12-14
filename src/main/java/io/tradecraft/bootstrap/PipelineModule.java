package io.tradecraft.bootstrap;

import io.tradecraft.observability.trace.TraceWriter;
import io.tradecraft.oms.dispatch.EffectPublisher;
import io.tradecraft.oms.dispatch.InboundDispatcher;
import io.tradecraft.oms.runtime.EventTranslator;
import io.tradecraft.oms.runtime.ParentFsmExecutor;
import io.tradecraft.oms.runtime.ParentFxProcessor;
import io.tradecraft.oms.runtime.Pipeline;
import io.tradecraft.oms.support.ThreadGuard;

public final class PipelineModule implements Lifecycle {
    private final ThreadGuard guard;
    private final String threadName;
    private InboundDispatcher inboundDispatcher;
    private EffectPublisher effectPublisher;
    private EventTranslator translator;
    private ParentFsmExecutor fsmExecutor;
    private ParentFxProcessor parentFxProcessor;
    private TraceWriter traceWriter;

    private Thread thread;
    private Pipeline pipeline;

    interface PipelineFactory {
        Pipeline create();
    }

    private final PipelineFactory pipelineFactory;

    public PipelineModule(ThreadGuard guard, String threadName,
                          InboundDispatcher inboundDispatcher, EffectPublisher effectPublisher,
                    EventTranslator eventTranslator, ParentFsmExecutor fsmExecutor,
                    ParentFxProcessor parentFxProcessor, TraceWriter traceWriter) {
        this(guard, threadName,
                () -> new Pipeline(inboundDispatcher, effectPublisher, eventTranslator, fsmExecutor, parentFxProcessor, traceWriter)
        );
    }

    // TEST-ONLY CONSTRUCTOR
    PipelineModule(
            ThreadGuard guard,
            String threadName,
            PipelineFactory pipelineFactory
    ) {
        this.guard = guard;
        this.threadName = threadName;
        this.pipelineFactory = pipelineFactory;
    }

    @Override
    public void start() {
        this.pipeline = pipelineFactory.create();

        thread = new Thread(() -> {
            guard.bindToCurrent();
            try {
                pipeline.run();
            } finally {
                guard.clear();
            }
        }, threadName);

        thread.setUncaughtExceptionHandler((t, e) -> e.printStackTrace(System.err));
        thread.start();
    }

    @Override
    public void stop() {
        if (pipeline != null) pipeline.stop();
        if (thread != null) thread.interrupt();
    }
}
