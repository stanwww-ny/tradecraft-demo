package io.tradecraft.fixqfj.outbound;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.log.LogUtils;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.common.utils.IdleStrategy;
import io.tradecraft.fixqfj.mapper.FixOrderTranslator;
import io.tradecraft.observability.trace.TraceWriter;
import io.tradecraft.oms.event.EventQueue;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.tradecraft.common.envelope.Stage.FIX_OUT;
import static io.tradecraft.common.meta.Component.OMS;
import static io.tradecraft.common.meta.Flow.IN;
import static io.tradecraft.common.meta.Flow.OUT;
import static io.tradecraft.common.meta.MessageType.ER;

public final class ExecReportSender implements Runnable {

    private final EventQueue<Envelope<PubExecReport>> execReportBus;
    private final ExecReportRouter router;

    private final FixOrderTranslator translator;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Counter erCount;
    private final Counter erError;
    private final Timer erTimer;
    private final EnvelopeMetaFactory metaFactory;
    private final TraceWriter traceWriter;
    private final IdleStrategy idle;
    /**
     * Prod default; tests can inject a stub.
     */
    private Sender sender = Session::sendToTarget;
    private Thread thread;

    public ExecReportSender(EventQueue<Envelope<PubExecReport>> execReportBus,
                            ExecReportRouter router,
                            MeterRegistry meterRegistry,
                            EnvelopeMetaFactory metaFactory,
                            TraceWriter traceWriter) {

        this(execReportBus, router, meterRegistry,
                new FixOrderTranslator(metaFactory.dualTimeSource()),
                Session::sendToTarget, metaFactory, traceWriter);
    }

    public ExecReportSender(EventQueue<Envelope<PubExecReport>> execReportBus,
                            ExecReportRouter router,
                            MeterRegistry meterRegistry,
                            FixOrderTranslator translator,
                            Sender sender,
                            EnvelopeMetaFactory metaFactory,
                            TraceWriter traceWriter) {
        this.metaFactory = metaFactory;
        this.traceWriter = traceWriter;
        this.translator = translator;
        this.execReportBus = execReportBus;
        this.router = router;
        this.sender = sender;
        this.erCount = meterRegistry != null ? Counter.builder("oms.out.er.sent").register(meterRegistry) : null;
        this.erError = meterRegistry != null ? Counter.builder("oms.out.er.errors").register(meterRegistry) : null;
        this.erTimer = meterRegistry != null ? Timer.builder("oms.out.er.latency").register(meterRegistry) : null;
        this.idle = IdleStrategy.defaultStrategy();
    }

    public void start(String name) {
        thread = new Thread(this, name);
        thread.start();
    }

    public void stop() {
        running.set(false);
        if (thread != null) thread.interrupt();
    }

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            boolean progressed = false;
            Envelope<PubExecReport> envelope = execReportBus.poll();
            if (envelope != null) {
                progressed = true;
                if (erTimer != null) {
                    erTimer.record(() -> { dispatch(envelope); });
                }
                else {
                    dispatch(envelope);
                }
                if (erCount != null) erCount.increment();
            }

            if (!progressed) {
                idle.idle();
            } else {
                idle.reset();
            }
        }
    }

    private void dispatch(Envelope<PubExecReport> envelope) {
        try {
            Meta meta = envelope.meta();
            PubExecReport er = envelope.payload();
            LogUtils.log(OMS, ER, IN, this, "ExecReport to send", er);
            metaFactory.addErHop(meta, er);
            send(er);
            metaFactory.addHop(meta, FIX_OUT);
            envelope.sealed();
            traceWriter.write(envelope);
            LogUtils.log(OMS, ER, OUT, this, "ExecReport sent", er);
        } catch (SessionNotFound e) {
            if (erError != null) erError.increment();
            LogUtils.log(OMS, ER, OUT, this, "Session not found. ExecReportSender Failed", e);
        }
    }

    private void send(PubExecReport er) throws SessionNotFound {
        Message m = translator.toExecutionReport(er);
        SessionID sid = router.route(er);
        sender.send(m, sid); // <<— seam replaces direct Session.sendToTarget(...)
    }

    public interface ExecReportRouter {
        SessionID route(PubExecReport er);
    }

    /**
     * Seam for sending FIX messages (prod default calls QuickFIX/J).
     */
    @FunctionalInterface
    public interface Sender {
        boolean send(Message m, SessionID sid) throws SessionNotFound;
    }
}
