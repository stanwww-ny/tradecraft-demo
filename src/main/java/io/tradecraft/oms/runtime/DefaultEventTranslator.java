package io.tradecraft.oms.runtime;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.allocator.ParentIdAllocator;
import io.tradecraft.fixqfj.session.ParentSessionBinder;
import io.tradecraft.oms.core.parentfx.CancelChildIfParentRequested;
import io.tradecraft.oms.core.parentfx.ParentFx;
import io.tradecraft.oms.event.EvBoundCancelReq;
import io.tradecraft.oms.event.EvBoundParentNew;
import io.tradecraft.oms.event.EvBoundReplaceReq;
import io.tradecraft.oms.event.EvCancelReq;
import io.tradecraft.oms.event.EvChildAck;
import io.tradecraft.oms.event.EvChildFill;
import io.tradecraft.oms.event.EvReplaceReq;
import io.tradecraft.oms.event.OrderEvent;
import io.tradecraft.oms.event.OrderEventFactory;
import io.tradecraft.oms.repo.ClOrdIndex;

import java.util.Collections;
import java.util.List;

public final class DefaultEventTranslator implements EventTranslator {

    private final ClOrdIndex clOrdIndex;
    private final ParentIdAllocator parentIdAllocator;
    private final ParentSessionBinder parentSessionBinder;
    private final ParentCancelRegistry parentCancelRegistry;
    private final EnvelopeMetaFactory metaFactory;
    private final ChildFillDeduper fillDeduper;

    private final OrderEventFactory orderEventFactory;

    public DefaultEventTranslator(
            ClOrdIndex clOrdIndex,
            ParentIdAllocator parentIdAllocator,
            ParentSessionBinder parentSessionBinder,
            ParentCancelRegistry parentCancelRegistry,
            EnvelopeMetaFactory metaFactory,
            ChildFillDeduper fillDeduper
    ) {
        this.clOrdIndex = clOrdIndex;
        this.parentIdAllocator = parentIdAllocator;
        this.parentSessionBinder = parentSessionBinder;
        this.parentCancelRegistry = parentCancelRegistry;
        this.metaFactory = metaFactory;
        this.fillDeduper = fillDeduper;
        this.orderEventFactory = new OrderEventFactory();
    }


    // ------------------------------------------------------------
    // 1) MAIN TRANSLATION LOGIC (Bound → OMS event)
    // ------------------------------------------------------------

    @Override
    public OrderEvent translate(Envelope<OrderEvent> env, Meta meta) {
        OrderEvent ev = env.payload();

        switch (ev) {

            case EvBoundParentNew p -> {
                return translateBoundParentNew(p, meta);
            }

            case EvBoundCancelReq c -> {
                return translateBoundCancelReq(c);
            }

            case EvBoundReplaceReq r -> {
                return translateBoundReplaceReq(r);
            }

            case EvChildFill cf -> {
                return translateChildFill(cf, meta);
            }

            default -> {
                return ev; // no translation needed
            }
        }
    }


    // ------------------------------------------------------------
    // 2) PRE-FSM FX (EvChildAck → CancelChildIfParentRequested)
    // ------------------------------------------------------------

    @Override
    public List<ParentFx> preFsmFx(Envelope<OrderEvent> env) {
        OrderEvent ev = env.payload();

        if (ev instanceof EvChildAck a) {
            return List.of(new CancelChildIfParentRequested(
                    a.parentId(),
                    a.childId(),
                    a.tsNanos()
            ));
        }

        return Collections.emptyList();
    }


    // ------------------------------------------------------------
    // TRANSLATION HELPERS (extracted from Pipeline)
    // ------------------------------------------------------------

    private OrderEvent translateBoundParentNew(EvBoundParentNew e, Meta meta) {
        SessionClOrdKey key = new SessionClOrdKey(e.sessionKey(), e.clOrdId());
        ParentId pid = clOrdIndex.get(key);

        if (pid == null) {
            pid = parentIdAllocator.allocate();
            ParentId prev = clOrdIndex.putIfAbsent(key, pid);
            if (prev != null)
                pid = prev;
            else
                parentSessionBinder.bindParent(pid, e.sessionKey());
        }

        //metaFactory.addHop(meta, e);
        return orderEventFactory.toEvNew(e, pid);
    }


    private OrderEvent translateBoundCancelReq(EvBoundCancelReq e) {
        SessionClOrdKey key = new SessionClOrdKey(e.sessionKey(), e.origClOrdId());
        ParentId pid = clOrdIndex.get(key);

        if (pid == null) {
            pid = parentIdAllocator.allocate();
            ParentId prev = clOrdIndex.putIfAbsent(key, pid);
            if (prev != null)
                pid = prev;
        }

        // Mark parent cancel early — moved from Pipeline
        parentCancelRegistry.mark(pid);

        return new EvCancelReq(
                pid,
                e.tsNanos(),
                e.clOrdId(),
                e.origClOrdId(),
                e.accountId(),
                e.domainAccountType(),
                e.instrumentKey(),
                e.side(),
                e.qty(),
                e.exDest(),
                e.reason()
        );
    }


    private OrderEvent translateBoundReplaceReq(EvBoundReplaceReq e) {
        SessionClOrdKey key = new SessionClOrdKey(e.sessionKey(), e.origClOrdId());
        ParentId pid = clOrdIndex.get(key);

        if (pid == null) {
            pid = parentIdAllocator.allocate();
            ParentId prev = clOrdIndex.putIfAbsent(key, pid);
            if (prev != null)
                pid = prev;
        }

        return new EvReplaceReq(
                pid,
                e.tsNanos(),
                e.clOrdId(),
                e.origClOrdId(),
                e.accountId(),
                e.domainAccountType(),
                e.instrumentKey(),
                e.side(),
                e.qty(),
                e.ordType(),
                e.limitPxMicros() == null ? 0 : e.limitPxMicros(),
                e.tif(),
                e.exDest()
        );
    }


    private OrderEvent translateChildFill(EvChildFill cf, Meta meta) {
        if (fillDeduper.isDuplicate(cf.childId(), cf.execId())) {
            //metaFactory.addHop(meta, cf); // mark duplicate hop
            return cf; // silently drop; Pipeline will ignore
        }

        //metaFactory.addHop(meta, cf);
        return cf;
    }
}
