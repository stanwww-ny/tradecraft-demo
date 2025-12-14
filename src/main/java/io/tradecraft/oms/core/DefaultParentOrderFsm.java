package io.tradecraft.oms.core;

import io.tradecraft.common.domain.market.ExecKind;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.common.spi.oms.intent.ParentRouteIntent;
import io.tradecraft.oms.core.parentfx.CancelAllActiveChildren;
import io.tradecraft.oms.core.parentfx.ParentCancelDone;
import io.tradecraft.oms.core.parentfx.WantParentCancel;
import io.tradecraft.oms.event.EvAck;
import io.tradecraft.oms.event.EvCancelAck;
import io.tradecraft.oms.event.EvCancelReq;
import io.tradecraft.oms.event.EvChildAck;
import io.tradecraft.oms.event.EvChildCanceled;
import io.tradecraft.oms.event.EvChildFill;
import io.tradecraft.oms.event.EvFill;
import io.tradecraft.oms.event.EvNew;
import io.tradecraft.oms.event.EvReject;
import io.tradecraft.oms.event.OrderEvent;

import java.util.List;
import java.util.Objects;

/**
 * FSM patched to work with the preserved (mutable) OrderState API.
 * - Uses beginCancel/markChildPendingCancel/markChildCanceled/recomputeFromChildren
 * - Emits ParentFx to let Pipeline perform side-effects + dedupe
 */
public final class DefaultParentOrderFsm implements ParentOrderFsm {

    @Override public Effects apply(OrderState st, OrderEvent ev) {
        Objects.requireNonNull(st); Objects.requireNonNull(ev);
        if (ev instanceof EvNew e)            return onNew(st, e);
        if (ev instanceof EvAck e)            return onAck(st, e);
        if (ev instanceof EvReject e)         return onReject(st, e);
        if (ev instanceof EvCancelReq e)      return onCancelReq(st, e);
        if (ev instanceof EvCancelAck e)      return onCancelAck(st, e);
        if (ev instanceof EvChildAck e)       return onChildAck(st, e);
        if (ev instanceof EvChildFill e)      return onChildFill(st, e);
        if (ev instanceof EvChildCanceled e)  return onChildCanceled(st, e);
        if (ev instanceof EvFill e)           return onFill(st, e);
        return Effects.none();
    }

    private Effects onNew(OrderState st, EvNew ev) {
        // Initialize parent-level state
        st.touch(ev.tsNanos());
        st.setStatus(OrderStatus.NEW_PENDING);
        ParentRouteIntent route = ParentRouteIntent.builder().parentId(ev.parentId())
                        .clOrdId(ev.clOrdId())
                        .accountId(ev.accountId()).accountType(ev.accountType())
                        .instrumentKey(ev.instrumentKey())
                        .side(ev.side()).parentQty(ev.qty()).leavesQty(ev.qty())
                        .ordType(ev.ordType()).limitPxMicros(ev.limitPxMicros())
                        .tif(ev.tif()).expireAt(ev.tif().computeExpireAt(ev.tsNanos(), null))
                        .exDest(ev.exDest())
                        .candidateVenues(List.of()).targetChildQty(null).maxParallelChildren(1)
                        .intentId(st.intentId()).tsNanos(ev.tsNanos()).build();
        return Effects.withState(st)
                .intents(route)              // tell planner to route children
                .build();                    // no ER yet
    }

    private Effects onAck(OrderState st, EvAck e) {
        if (st.isDone()) return Effects.none();
        st.setStatus(OrderStatus.WORKING);
        st.setLastTsNanos(e.tsNanos());

        // 4) Quantities for the ER (best-effort fallbacks)
        long cum    = st.cumQty();
        long total  = st.orderQty();             // assuming you add totalQty()
        long leaves = st.leavesQty();            // always defined

// if you want a fallback definition of leaves from total-cum:
        if (leaves == 0 && total > 0) {
            leaves = Math.max(0L, total - cum);
        }
        st.setLeavesQty(leaves);

        PubExecReport er = PubExecReport.builder()
                .parentId(st.parentId())
                .clOrdId(st.clOrdId())
                .instrumentKey(st.instrumentKey())
                .domainSide(st.side())
                .execId(e.execId())
                .execKind(ExecKind.ACK)
                .status(st.status())
                .cumQty(st.cumQty())
                .leavesQty(st.leavesQty())
                .avgPxMicros(st.avgPxMicros())
                .tsNanos(e.tsNanos())
                .build();
        return Effects.withState(st).er(er).build();
    }

    private Effects onReject(OrderState st, EvReject e) {
        if (st.isDone()) return Effects.none();
        st.setStatus(OrderStatus.REJECTED);
        st.setDoneTsNanos(e.tsNanos());
        st.setLastTsNanos(e.tsNanos());
        PubExecReport er = PubExecReport.builder()
                        .parentId(st.parentId())
                        .clOrdId(st.clOrdId())
                        .instrumentKey(st.instrumentKey())
                        .domainSide(st.side())
                        .execKind(ExecKind.REJECTED)
                        .status(st.status())
                        .cumQty(st.cumQty())
                        .leavesQty(st.leavesQty())
                        .avgPxMicros(st.avgPxMicros())
                        .tsNanos(e.tsNanos())
                        .reason(e.reason())
                        .build();
        return Effects.withState(st).er(er).build();
    }

    private Effects onCancelReq(OrderState st, EvCancelReq e) {
        if (st.isDone()) return Effects.none();
        st.beginCancel(e.tsNanos());
        // Let pipeline handle child cancels + dedupe
        return Effects.withState(st).parentFxes(List.of(
                new WantParentCancel(e.parentId(), e.tsNanos()),
                new CancelAllActiveChildren(e.parentId(), e.tsNanos())
        )).build();
    }

    private Effects onCancelAck(OrderState st, EvCancelAck e) {
        if (st.isDone()) return Effects.none();
        // Informational; parent stays PENDING_CANCEL until children are terminal
        PubExecReport er = PubExecReport.builder()
                .parentId(st.parentId())
                .clOrdId(st.clOrdId())
                .instrumentKey(st.instrumentKey())
                .domainSide(st.side())
                .execKind(ExecKind.PENDING_CANCEL)
                .status(st.status())
                .cumQty(st.cumQty())
                .leavesQty(st.leavesQty())
                .avgPxMicros(st.avgPxMicros())
                .tsNanos(e.tsNanos())
                .build();
        return Effects.withState(st).er(er).build();
    }

    private Effects onChildAck(OrderState st, EvChildAck e) {
        if (st.isDone()) return Effects.none();
        st.addChildAck(e.childId(), e.venueOrderId(), e.tsNanos());
        EvAck parentAck = EvAck.builder().parentId(e.parentId()).execId(e.execId()).tsNanos(e.tsNanos()).build();

        // Late-ack hook: ask pipeline to cancel this child if parent is canceling
        return Effects.withState(st).followUps(parentAck).build();
    }

    private Effects onChildFill(OrderState st, EvChildFill e) {
        if (st.isDone()) return Effects.none();
        st.accumulateChildFill(e.childId(), e.lastQty(), e.lastPxMicros(), e.tsNanos());
        st.setLastTsNanos(e.tsNanos());
        st.recomputeFromChildren();
        ExecKind kind = (st.leavesQty() == 0) ? ExecKind.FILL : ExecKind.PARTIAL_FILL;
        if (st.leavesQty() == 0) {
            st.setStatus(OrderStatus.FILLED);
            st.setDoneTsNanos(e.tsNanos());
        } else {
            st.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
        PubExecReport er = PubExecReport.builder()
                .parentId(st.parentId())
                .clOrdId(st.clOrdId())
                .instrumentKey(st.instrumentKey())
                .domainSide(st.side())
                .execKind(kind)
                .status(st.status())
                .execId(e.execId())
                .cumQty(st.cumQty())
                .leavesQty(st.leavesQty())
                .avgPxMicros(st.avgPxMicros())
                .tsNanos(e.tsNanos())
                .lastQty(e.lastQty())
                .lastPxMicros(e.lastPxMicros())
                .build();
        return Effects.withState(st).er(er).build();
    }

    private Effects onFill(OrderState st, EvFill e) {
        if (st.isDone()) return Effects.none();

        final long ts        = e.tsNanos();
        final long prevCum   = st.cumQty();
        final long delta     = e.lastQty();
        final long pxMicros  = e.lastPxMicros();   // keep price in micros as long

        final long newCum    = prevCum + delta;
        final long newLeaves = Math.max(0L, st.leavesQty() - delta);

        // update parent aggregates
        st.setCumQty(newCum);
        st.setLeavesQty(newLeaves);
        st.setAvgPxMicros(recomputeAvgPxMicros(st.avgPxMicros(), prevCum, pxMicros, delta));
        st.setLastTsNanos(ts);

        final boolean isFinal = (newLeaves == 0L);
        if (!isFinal) {
            // promote only if coming from pending/acked
            final OrderStatus s = st.status();
            if (s == OrderStatus.ACKED || s == OrderStatus.NEW_PENDING) {
                st.setStatus(OrderStatus.PARTIALLY_FILLED);
            }

            PubExecReport er = PubExecReport.builder()
                    .parentId(st.parentId()).clOrdId(st.clOrdId()).childId(e.childId())
                    .instrumentKey(st.instrumentKey()).domainSide(st.side())
                    .execKind(ExecKind.PARTIAL_FILL)
                    .status(st.status())
                    .execId(e.execId())
                    .lastQty(delta).cumQty(newCum).leavesQty(newLeaves).lastPxMicros(e.lastPxMicros())
                    .avgPxMicros(st.avgPxMicros()).tsNanos(ts).build();
            return Effects.withState(st).er(er).build();
        } else {
            st.setStatus(OrderStatus.FILLED);
            if (st.isDone()) {
                st.setDoneTsNanos(ts);
            }

            PubExecReport er = PubExecReport.builder()
                    .parentId(st.parentId()).clOrdId(st.clOrdId()).childId(e.childId())
                    .instrumentKey(st.instrumentKey()).domainSide(st.side())
                    .execKind(ExecKind.FILL)
                    .status(st.status())
                    .execId(e.execId())
                    .lastQty(delta).cumQty(newCum).leavesQty(0L).lastPxMicros(e.lastPxMicros())
                    .avgPxMicros(st.avgPxMicros()).tsNanos(ts).build();
            return Effects.withState(st).er(er).build();
        }
    }

    private long recomputeAvgPxMicros(long prevAvg, long prevCum, long px, long delta) {
        if (delta <= 0) return prevCum > 0 ? prevAvg : 0L;
        long newCum = prevCum + delta;
        if (newCum <= 0) return 0L;
        long prevNotional = prevAvg * prevCum;
        long addNotional  = px * delta;
        long num = prevNotional + addNotional;
        return (num + newCum / 2) / newCum; // round-to-nearest
    }

    private Effects onChildCanceled(OrderState st, EvChildCanceled e) {
        if (st.isDone()) return Effects.none();
        st.markChildCanceled(e.childId(), e.tsNanos());
        st.recomputeFromChildren();
        if (st.allChildrenTerminal()) {
            st.markCanceled(e.tsNanos());
            PubExecReport er = PubExecReport.builder()
                    .parentId(st.parentId())
                    .clOrdId(st.clOrdId())
                    .instrumentKey(st.instrumentKey())
                    .domainSide(st.side())
                    .execKind(ExecKind.CANCELED)
                    .execId(e.execId())
                    .status(st.status())
                    .cumQty(st.cumQty())
                    .leavesQty(st.leavesQty())
                    .avgPxMicros(st.avgPxMicros())
                    .tsNanos(e.tsNanos())
                    .build();
            return Effects.withState(st)
                    .er(er)
                    .parentFxes(List.of(new ParentCancelDone(e.parentId(), e.tsNanos())))
                    .build();
        }
        return Effects.withState(st).build();
    }

    private static PubExecReport er(OrderState st, ExecKind kind, long ts) {
        return PubExecReport.builder()
                .parentId(st.parentId())
                .clOrdId(st.clOrdId())
                .instrumentKey(st.instrumentKey())
                .domainSide(st.side())
                .execKind(kind)
                .status(st.status())
                .cumQty(st.cumQty())
                .leavesQty(st.leavesQty())
                .avgPxMicros(st.avgPxMicros())
                .tsNanos(ts)
                .build();
    }
}
