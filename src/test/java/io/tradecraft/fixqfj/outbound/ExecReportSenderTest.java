package io.tradecraft.fixqfj.outbound;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.id.IdFactory;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.fixqfj.mapper.FixOrderTranslator;
import io.tradecraft.observability.trace.TraceWriter;
import io.tradecraft.oms.event.EventQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.Message;
import quickfix.SessionID;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecReportSenderTest {

    private ExecReportSender drainer;
    private IdFactory ids;
    private EnvelopeMetaFactory metaFactory;
    private DualTimeSource dualTimeSource;
    private TraceWriter traceWriter;

    @BeforeEach
    void setUp() {
        ids = IdFactory.testIds(42L);
        dualTimeSource = TestClocks.msTicker();
        metaFactory = new EnvelopeMetaFactory(ids.envelopeSeq(), dualTimeSource);
        traceWriter = mock(TraceWriter.class);
     }

    private static EventQueue<Envelope<PubExecReport>> blockingErBus(BlockingQueue<Envelope<PubExecReport>> backing) {
        return new EventQueue<>() {
            @Override
            public boolean offer(Envelope<PubExecReport> item) {
                return backing.offer(item);
            }

            @Override
            public Envelope<PubExecReport> poll() {
                return backing.poll();
            }

            @Override
            public Envelope<PubExecReport> poll(long timeout, TimeUnit unit) throws InterruptedException {
                return backing.poll(timeout, unit);
            }

            @Override
            public int size() {
                return backing.size();
            }
        };
    }

    @AfterEach
    void tearDown() {
        if (drainer != null) drainer.stop();
    }

    @Test
    void happyPath_sendsExecReport_viaInjectedSender() throws Exception {
        BlockingQueue<Envelope<PubExecReport>> q = new ArrayBlockingQueue<>(1);
        EventQueue<Envelope<PubExecReport>> erBus = blockingErBus(q);

        ExecReportSender.ExecReportRouter router = mock(ExecReportSender.ExecReportRouter.class);
        SessionID sid = new SessionID("FIX.4.4", "OMS", "VENUE");
        when(router.route(any())).thenReturn(sid);

        FixOrderTranslator fakeTranslator = mock(FixOrderTranslator.class);
        when(fakeTranslator.toExecutionReport(any())).thenReturn(new Message());

        AtomicReference<Message> sentMsg = new AtomicReference<>();
        AtomicReference<SessionID> sentSid = new AtomicReference<>();
        ExecReportSender.Sender sender = (m, s) -> {
            sentMsg.set(m);
            sentSid.set(s);
            return true;
        };

        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        DualTimeSource dualTimeSource = mock(DualTimeSource.class);

        drainer = new ExecReportSender(erBus, router, meters, fakeTranslator, sender, metaFactory, traceWriter);
        drainer.start("er-drainer-happy");

        PubExecReport er = mock(PubExecReport.class); // no field stubs needed with fakeTranslator
        Meta meta = metaFactory.newMeta();
        Envelope<PubExecReport> env = Envelope.of(er, meta);
        q.put(env);

        Thread.sleep(150);

        verify(router, atLeastOnce()).route(er);
        assertNotNull(sentMsg.get(), "expected a message to be sent");
        assertEquals(sid, sentSid.get(), "sender should receive router's SessionID");
        assertTrue(meters.find("oms.out.er.sent").counter().count() >= 1.0);
    }

    @Test
    void errorPath_incrementsErrorCounter_whenSenderThrows() throws Exception {
        BlockingQueue<Envelope<PubExecReport>> q = new ArrayBlockingQueue<>(1);
        EventQueue<Envelope<PubExecReport>> erBus = blockingErBus(q);

        ExecReportSender.ExecReportRouter router = mock(ExecReportSender.ExecReportRouter.class);
        when(router.route(any())).thenReturn(new SessionID("FIX.4.4", "OMS", "VENUE"));

        FixOrderTranslator fakeTranslator = mock(FixOrderTranslator.class);
        when(fakeTranslator.toExecutionReport(any())).thenReturn(new Message());

        ExecReportSender.Sender badSender = (m, s) -> {
            throw new quickfix.SessionNotFound("no session");
        };

        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        DualTimeSource dualTimeSource = mock(DualTimeSource.class);

        drainer = new ExecReportSender(erBus, router, meters, fakeTranslator, badSender, metaFactory, traceWriter);
        drainer.start("er-drainer-error");

        PubExecReport er = mock(PubExecReport.class);
        Meta meta = metaFactory.newMeta();
        Envelope<PubExecReport> env = Envelope.of(er, meta);
        q.put(env);

        Thread.sleep(150);

        assertTrue(meters.find("oms.out.er.errors").counter().count() >= 1.0, "expected an ER error recorded");
    }
}
