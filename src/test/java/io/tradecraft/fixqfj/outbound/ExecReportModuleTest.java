// src/test/java/org/example/v4/bootstrap/ErDrainerModuleTest.java
package io.tradecraft.fixqfj.outbound;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.tradecraft.bootstrap.ExecReportModule;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.JdkQueueChannel;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.id.IdFactory;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.fixqfj.session.SessionResolver;
import io.tradecraft.observability.trace.TraceWriter;
import io.tradecraft.oms.event.EventQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.SessionID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

// New ErDrainerModule ctor taking a resolver
class ExecReportModuleTest {
    IdFactory ids;
    DualTimeSource dualTimeSource;
    EnvelopeMetaFactory metaFactory;
    Meta meta;
    TraceWriter traceWriter;

    @BeforeEach
    void setUp() {
        ids = IdFactory.testIds(42L);
        dualTimeSource = TestClocks.msTicker();
        metaFactory = new EnvelopeMetaFactory(ids.envelopeSeq(), dualTimeSource);
        meta = metaFactory.newMeta();
        traceWriter = mock(TraceWriter.class);
    }


    @Test
    void start_and_stop_drainer() {
        EventQueue<Envelope<PubExecReport>> erBus = new JdkQueueChannel<>();
        var meters = new SimpleMeterRegistry();

        SessionResolver fakeResolver = er -> new SessionID("FIX.4.4", "OMS", "CLIENT");
        var execReportModule = new ExecReportModule(erBus, fakeResolver, meters, metaFactory, traceWriter);

        assertDoesNotThrow(execReportModule::start);
        assertDoesNotThrow(execReportModule::stop);
    }
}
