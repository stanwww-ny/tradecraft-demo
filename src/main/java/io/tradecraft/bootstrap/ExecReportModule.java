package io.tradecraft.bootstrap;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.log.LogUtils;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.fixqfj.outbound.ExecReportSender;
import io.tradecraft.fixqfj.session.SessionResolver;
import io.tradecraft.observability.trace.TraceWriter;
import io.tradecraft.oms.event.EventQueue;

import static io.tradecraft.common.meta.Component.ER;
import static io.tradecraft.common.meta.Flow.NA;
import static io.tradecraft.common.meta.MessageType.ADMIN;

public final class ExecReportModule implements Lifecycle {
    private final EventQueue<Envelope<PubExecReport>> execReportBus;
    private final SessionResolver resolver;
    private final SimpleMeterRegistry meters;
    private final EnvelopeMetaFactory envelopeMetaFactory;
    private final TraceWriter traceWriter;

    private ExecReportSender execReportSender;

    public ExecReportModule(EventQueue<Envelope<PubExecReport>> execReportBus,
                            SessionResolver resolver,
                            SimpleMeterRegistry meters,
                            EnvelopeMetaFactory metaFactory,
                            TraceWriter traceWriter) {
        this.execReportBus = execReportBus;
        this.resolver = resolver;
        this.meters = meters;
        this.envelopeMetaFactory = metaFactory;
        this.traceWriter = traceWriter;
    }

    @Override
    public void start() {
        execReportSender = new ExecReportSender(execReportBus, resolver::resolve, meters, envelopeMetaFactory, traceWriter);
        execReportSender.start("er-sender");
    }

    @Override
    public void stop() {
        if (execReportSender != null) {
            try {
                execReportSender.stop();
                traceWriter.close();
            } catch (Exception e) {
                LogUtils.logEx(ER, ADMIN, NA, this, e);
            }
        }
    }
}
