package io.tradecraft.fixqfj.acceptor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMeta;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Stage;
import io.tradecraft.fixqfj.event.FixEvCancelReq;
import io.tradecraft.fixqfj.event.FixEvInbound;
import io.tradecraft.fixqfj.event.FixEvParentNew;
import io.tradecraft.fixqfj.event.FixEvReplaceReq;
import io.tradecraft.fixqfj.mapper.FixInboundMapper;
import io.tradecraft.fixqfj.session.SessionIndex;
import io.tradecraft.fixqfj.session.SessionKey;
import io.tradecraft.oms.event.EvBoundCancelReq;
import io.tradecraft.oms.event.EvBoundParentNew;
import io.tradecraft.oms.event.EvBoundReplaceReq;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.fix44.MessageCracker;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReplaceRequest;
import quickfix.fix44.OrderCancelRequest;

public class OmsFixInbound extends MessageCracker implements Application {
    private static final Logger log = LoggerFactory.getLogger(OmsFixInbound.class);

    private final Timer cbTimer;
    private final Timer nosTimer, ocrTimer, ocrrTimer;
    private final Counter cbErrors, drops;
    private final EventQueue<Envelope<OrderEvent>> inboundBus;
    private final SessionIndex sessionIndex;
    private final EnvelopeMetaFactory metaFactory;

    public OmsFixInbound(EventQueue<Envelope<OrderEvent>> inboundBus, EnvelopeMetaFactory metaFactory, SessionIndex sessionIndex, MeterRegistry registry) {
        this.inboundBus = inboundBus;
        this.cbTimer = Timer.builder("inbound.callback.nanos").description("fromApp→crack duration").register(registry);
        this.nosTimer = Timer.builder("inbound.nos.handler.nanos").register(registry);
        this.ocrTimer = Timer.builder("inbound.ocr.handler.nanos").register(registry);
        this.ocrrTimer = Timer.builder("inbound.ocrr.handler.nanos").register(registry);
        this.cbErrors = Counter.builder("inbound.callback.errors").register(registry);
        this.drops = Counter.builder("inbound.offer.fail.full").register(registry);
        this.sessionIndex = sessionIndex;
        this.metaFactory = metaFactory;
    }

    @Override
    public void fromApp(Message msg, SessionID sid) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        cbTimer.record(() -> {
            try {
                crack(msg, sid);
            } catch (RuntimeException | FieldNotFound | UnsupportedMessageType | IncorrectTagValue e) {
                cbErrors.increment();
                throw new RuntimeException(e);
            } finally {
                MDC.clear();
            }
        });
    }


    public void onMessage(NewOrderSingle m, SessionID sid) throws FieldNotFound {
        onMessage(m, sid, nosTimer);
    }


    public void onMessage(OrderCancelRequest m, SessionID sid) throws FieldNotFound {
        onMessage(m, sid, ocrTimer);
    }


    public void onMessage(OrderCancelReplaceRequest m, SessionID sid) throws FieldNotFound {
        onMessage(m, sid, ocrrTimer);
    }

    private void onMessage(Message m, SessionID sid, Timer timer) throws FieldNotFound {
        EnvelopeMeta meta = metaFactory.newMeta();
        metaFactory.addHop(meta, Stage.FIX_RECV, meta.createdNano);

        try {
            timer.recordCallable(() -> {
                try {
                    offer(m, sid, meta);
                    return null;
                } finally {
                    MDC.clear();
                }
            });
        } catch (Exception e) {
            if (e instanceof FieldNotFound f) throw f;
            throw new RuntimeException(e);
        }
    }

    private void offer(Message m, SessionID sid, EnvelopeMeta meta) throws FieldNotFound {
        FixEvInbound fev = null;
        if (m instanceof NewOrderSingle) {
            fev = FixInboundMapper.mapNos(m, sid, metaFactory.dualTimeSource().nowNanos());
        }
        if (m instanceof OrderCancelRequest) {
            fev = FixInboundMapper.mapCancel(m, sid, metaFactory.dualTimeSource().nowNanos());
        }
        if (m instanceof OrderCancelReplaceRequest) {
            fev = FixInboundMapper.mapReplace(m, sid, metaFactory.dualTimeSource().nowNanos());
        }
        if (fev == null) {
            throw new FieldNotFound("Message is not supported");
        }
        metaFactory.addHop(meta, Stage.FIX_TO_EVENT);
        OrderEvent ev = map(fev);
        Envelope<OrderEvent> envelope = new Envelope<>(ev, meta);
        metaFactory.addHop(meta, Stage.EV_RECV);
        if (!inboundBus.offer(envelope)) {
            warnDrop(m.getClass().getTypeName(), fev.clOrdId().value());
        }
    }


    private void warnDrop(String kind, String clOrdId) {
        drops.increment();
        log.warn("Inbound queue full; dropped {} clOrdId={}", kind, clOrdId);
    }


    @Override
    public void onCreate(SessionID id) {
    }

    @Override
    public void onLogon(SessionID sid) {// e.g., FIX.4.4:OMS->TRADER[:SIM]
        log.info("Logon: (sid={})", sid);
        sessionIndex.putSession(SessionKey.of(sid), sid);
    }

    @Override
    public void onLogout(SessionID id) {
        sessionIndex.removeAllFor(id);
    }

    @Override
    public void toAdmin(Message m, SessionID id) {
    }

    @Override
    public void fromAdmin(Message m, SessionID id) {
    }

    @Override
    public void toApp(Message m, SessionID id) throws DoNotSend {
    }


    private OrderEvent map(FixEvInbound ev) {
        SessionKey sessionKey = ev.sessionKey().reverse();

        return switch (ev) {
            case FixEvParentNew e -> new EvBoundParentNew(
                    null,
                    e.ingressNanos(),
                    sessionKey,
                    e.clOrdId(),
                    e.accountId(),
                    e.domainAccountType(),
                    e.instrumentKey(),
                    e.side(),
                    e.qty(),
                    e.ordType(),
                    e.ordType() == DomainOrdType.MARKET ? 0L : e.limitPxMicros(),
                    e.tif(),
                    e.exDest()
            );

            case FixEvCancelReq e -> new EvBoundCancelReq(
                    null,
                    e.ingressNanos(),
                    sessionKey,
                    e.clOrdId(),
                    e.origClOrdId() != null ? e.origClOrdId()
                            : e.clOrdId(),
                    e.accountId(),
                    e.domainAccountType(),
                    e.instrumentKey(),
                    e.side(),
                    e.qty(),
                    "",
                    ""
            );

            case FixEvReplaceReq e -> new EvBoundReplaceReq(
                    null,
                    e.ingressNanos(),
                    sessionKey,
                    e.clOrdId(),
                    e.origClOrdId() != null ? e.origClOrdId()
                            : e.clOrdId(),
                    e.accountId(),
                    e.domainAccountType(),
                    e.instrumentKey(),
                    e.side(),
                    e.qty(),
                    e.ordType(),
                    e.limitPxMicros(),
                    e.tif(),
                    e.exDest()
            );
        };
    }


}