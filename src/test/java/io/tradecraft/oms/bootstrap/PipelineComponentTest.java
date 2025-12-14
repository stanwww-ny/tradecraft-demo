// src/test/java/org/example/v4/bootstrap/PipelineModuleTest.java
package io.tradecraft.oms.bootstrap;

class PipelineComponentTest {

    /*
    @Test
    void start_and_stop_pipeline_thread() {
        IdFactory ids = IdFactory.testIds(42L);
        EventQueue<Envelope<OrderEvent>> inboundBus = new JdkBlockingChannel<>();
        EventQueue<Envelope<OrderEvent>> sorEvt = new JdkBlockingChannel<>();
        EventQueue<Envelope<PubExecReport>> erBus = new JdkBlockingChannel<>();
        EventQueue<Envelope<PubParentIntent>> intentBus = new JdkBlockingChannel<>();
        QueueManager queueManager = new QueueManager(inboundBus, sorEvt, erBus, intentBus);
        SessionIndex sessionIndex = new SessionIndex();
        var metaFactory = mock(EnvelopeMetaFactory.class);
        var internalTraceWriter = mock(TraceWriter.class);
        var guard = new StrictThreadGuard("Pipeline");
        var module = new PipelineModule(queueManager, guard, "Pipeline-UT-0", ids.parent(), ids.intent(),
                (ParentSessionBinder) sessionIndex, metaFactory, internalTraceWriter);

        assertDoesNotThrow(module::start);
        TestUtils.sleepQuietly(java.time.Duration.ofMillis(200));

        assertTrue(TestUtils.anyThreadWithPrefix("Pipeline-UT"), "pipeline thread should be alive");
        assertDoesNotThrow(module::stop);
    }

     */
}
