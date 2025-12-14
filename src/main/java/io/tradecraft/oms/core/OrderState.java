package io.tradecraft.oms.core;

import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.IntentId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueOrderId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * OrderState — preserves your original fields & methods, adds child cancel lifecycle helpers
 * and minimal terminal/active semantics without changing your public API contracts.
 *
 * Concurrency: single-writer only (pipeline thread). Not thread-safe.
 */
public final class OrderState {
    private final ParentId parentId;
    private final ClOrdId clOrdId;
    private final InstrumentKey instrumentKey;
    private final DomainSide domainSide;      // BUY/SELL, etc.
    private final DomainTif parentTif;
    private final Instant goodTilTime;        // only for GTD/GTT; otherwise null

    // Parent qty target (mutable to support replace)
    private long orderQty;                    // parent target qty (>0)

    // Aggregated (parent) execution state
    private long cumQty;
    private long leavesQty;
    private long avgPxMicros;                 // VWAP in micros
    private long lastTsNanos;

    // Keep exact parent notional for accurate delta pricing (sum child avg * cum)
    private long parentNotionalMicros;

    // Last delta captured by recomputeFromChildren()
    private long lastDeltaQty;
    private long lastDeltaPxMicros;           // weighted price for the last delta qty

    private Long firstAckTsNanos;             // null until first child ack
    private long firstFillTsNanos;
    private long lastFillTsNanos;
    private long cancelReqTsNanos;
    private long doneTsNanos;
    private OrderStatus status = OrderStatus.NEW;
    private IntentId intentId;

    // Child states by id
    private final Map<ChildId, ChildState> children = new HashMap<>();

    public OrderState(ParentId parentId,
                      ClOrdId clOrdId,
                      InstrumentKey instrumentKey,
                      DomainSide domainSide,
                      DomainTif parentTif,
                      Instant goodTilTime,
                      long orderQty,
                      long tsNanos,
                      IntentId intentId) {

        this.parentId = Objects.requireNonNull(parentId, "parentId");
        this.clOrdId = Objects.requireNonNull(clOrdId, "clOrdId");
        this.instrumentKey = Objects.requireNonNull(instrumentKey, "instrumentKey");
        this.domainSide = Objects.requireNonNull(domainSide, "orderSide");
        this.parentTif = Objects.requireNonNull(parentTif, "orderTimeInForce");
        this.goodTilTime = (parentTif == DomainTif.GTD) ?
                Objects.requireNonNull(goodTilTime, "goodTilTime required for GTD/GTT") : null;
        if (orderQty <= 0) throw new IllegalArgumentException("orderQty must be > 0");
        this.orderQty = orderQty;
        this.leavesQty = orderQty;
        this.lastTsNanos = tsNanos;
        this.intentId = intentId;
    }

    // ---- Child API expected by caller (PRESERVED) ----

    /** Returns the child state (creating it if absent). */
    public ChildState child(ChildId childId) {
        return children.computeIfAbsent(childId, ChildState::new);
    }

    /** Backward-compatible helper if caller prefers a single-call update. */
    public void accumulateChildFill(ChildId childId, long lastQty, long lastPxMicros, long tsNanos) {
        child(childId).accumulate(lastQty, lastPxMicros, tsNanos);
    }

    /** For deriving the parent "last" price after a recompute. */
    public long weightedLastPriceMicrosFromChildren(long lastQty) {
        if (lastQty == lastDeltaQty && lastDeltaQty > 0) return lastDeltaPxMicros;
        // Fallback to current parent average if delta tracking not available
        return avgPxMicros;
    }

    // ---- Child bookkeeping (PRESERVED + ENHANCED) ----

    public void addChildAck(ChildId childId, VenueOrderId venueOrderId, long tsNanos) {
        ChildState cs = children.computeIfAbsent(childId, ChildState::new);
        cs.venueOrderId = venueOrderId;
        cs.acked = true;
        cs.lastTsNanos = tsNanos;
        if (this.firstAckTsNanos == null) this.firstAckTsNanos = tsNanos;
        this.lastTsNanos = Math.max(this.lastTsNanos, tsNanos);
    }

    /** New: mark a child in PENDING_CANCEL; does not finalize terminality. */
    public void markChildPendingCancel(ChildId childId, long tsNanos) {
        ChildState cs = children.computeIfAbsent(childId, ChildState::new);
        if (cs.isTerminal()) return; // idempotent
        cs.pendingCancel = true;
        cs.lastTsNanos = tsNanos;
        this.lastTsNanos = Math.max(this.lastTsNanos, tsNanos);
    }

    /** New: finalize a child cancel (terminal). Zero remaining working qty at child-level semantics. */
    public void markChildCanceled(ChildId childId, long tsNanos) {
        ChildState cs = children.computeIfAbsent(childId, ChildState::new);
        if (cs.isTerminal()) return; // idempotent
        cs.pendingCancel = false;
        cs.canceled = true;
        cs.lastTsNanos = tsNanos;
        this.lastTsNanos = Math.max(this.lastTsNanos, tsNanos);
        // Parent aggregates are recomputed by caller via recomputeFromChildren() if needed.
    }

    /** New: mark a child new/reject/expired as terminal (utility if your venue emits those). */
    public void markChildRejected(ChildId childId, long tsNanos) {
        ChildState cs = children.computeIfAbsent(childId, ChildState::new);
        if (cs.isTerminal()) return;
        cs.rejected = true;
        cs.pendingCancel = false;
        cs.lastTsNanos = tsNanos;
        this.lastTsNanos = Math.max(this.lastTsNanos, tsNanos);
    }

    public void recomputeFromChildren() {
        long totalCum = 0L;
        long totalNotional = 0L;
        for (ChildState cs : children.values()) {
            if (cs.cumQty > 0) {
                totalCum += cs.cumQty;
                totalNotional += (long) cs.avgPxMicros * cs.cumQty;
            }
        }

        long prevCum = this.cumQty;
        long prevNotional = this.parentNotionalMicros;

        // capture delta BEFORE overwriting aggregates
        long deltaQty = totalCum - prevCum;
        long deltaNotional = totalNotional - prevNotional;
        if (deltaQty > 0 && deltaNotional > 0) {
            this.lastDeltaQty = deltaQty;
            this.lastDeltaPxMicros = (int) (deltaNotional / deltaQty);
        } else {
            this.lastDeltaQty = 0;
            this.lastDeltaPxMicros = 0;
        }

        this.cumQty = totalCum;
        this.parentNotionalMicros = totalNotional;
        this.leavesQty = Math.max(0L, orderQty - totalCum);
        if (totalCum > 0) {
            this.avgPxMicros = (int) (totalNotional / totalCum);
            if (this.firstFillTsNanos == 0L) this.firstFillTsNanos = this.lastTsNanos; // caller should set ts
            this.lastFillTsNanos = this.lastTsNanos;
        }
    }

    /** Utility: true when parent is terminal. */
    public boolean isDone() {
        return status == OrderStatus.FILLED
                || status == OrderStatus.CANCELED
                || status == OrderStatus.REJECTED
                || status == OrderStatus.EXPIRED;
    }

    public boolean isTrace() {
        return status == OrderStatus.ACKED
                || status == OrderStatus.FILLED
                || status == OrderStatus.CANCELED
                || status == OrderStatus.REJECTED
                || status == OrderStatus.EXPIRED;
    }

    /** Utility: all children terminal (FILLED/CANCELED/REJECTED/EXPIRED). */
    public boolean allChildrenTerminal() {
        for (ChildState cs : children.values()) if (!cs.isTerminal()) return false; return true;
    }

    // ---- Getters (PRESERVED) ----

    public ParentId parentId() { return parentId; }
    public ClOrdId clOrdId() { return clOrdId; }
    public InstrumentKey instrumentKey() { return instrumentKey; }
    public DomainSide side() { return domainSide; }
    public long orderQty() { return orderQty; }

    public long cumQty() { return cumQty; }
    public long leavesQty() { return leavesQty; }
    public long avgPxMicros() { return avgPxMicros; }
    public Long firstAckTsNanos() { return firstAckTsNanos; }
    public long cancelReqTsNanos() { return cancelReqTsNanos; }
    public long lastTsNanos() { return lastTsNanos; }
    public long doneTsNanos() { return doneTsNanos; }

    public OrderStatus status() { return status; }
    void setStatus(OrderStatus s) { this.status = s; }
    public IntentId intentId() { return intentId; }

    public void setCumQty(long cumQty) { this.cumQty = cumQty; }
    public void setLeavesQty(long leavesQty) { this.leavesQty = leavesQty; }
    public void setAvgPxMicros(long avgPxMicros) { this.avgPxMicros = avgPxMicros; }
    public void setLastTsNanos(long lastTsNanos) { this.lastTsNanos = lastTsNanos; }
    public void setFirstAckTsNanos(long firstAckTsNanos) { this.firstAckTsNanos = firstAckTsNanos; }
    public void setCancelReqTsNanos(long cancelReqTsNanos) { this.cancelReqTsNanos = cancelReqTsNanos; }
    public void setDoneTsNanos(long doneTsNanos) { this.doneTsNanos = doneTsNanos; }

    public Map<ChildId, ChildState> children() { return children; }

    // ---- Helpers ----
    void touch(long ts) { this.lastTsNanos = ts; }

    // ---- ChildState ----
    public static final class ChildState {
        private final ChildId childId;
        private VenueOrderId venueOrderId; // nullable
        private boolean acked;
        private boolean pendingCancel;      // NEW: indicates cancel in-flight
        private boolean canceled;           // NEW: terminal
        private boolean rejected;           // NEW: terminal
        private boolean expired;            // NEW: terminal (if you use it)
        private long cumQty;
        private long avgPxMicros;
        private long lastTsNanos;
        private DomainTif tif;
        private Instant expireAt;

        ChildState(ChildId childId) { this.childId = childId; }

        ChildState(ChildId childId, long lastTsNanos, DomainTif tif, Instant expireAt) {
            this.childId = Objects.requireNonNull(childId, "childId");
            this.tif = Objects.requireNonNull(tif, "tif");
            this.expireAt = expireAt; // nullable for IOC/FOK immediate semantics
            this.cumQty = 0;
            this.avgPxMicros = 0;
            this.lastTsNanos = lastTsNanos;
        }

        public void accumulate(long lastQty, long lastPxMicros, long tsNanos) {
            if (isTerminal()) return; // no fills after terminal
            if (lastQty <= 0 || lastPxMicros <= 0) return;
            long newCum = this.cumQty + lastQty;
            long notional = (long) this.avgPxMicros * this.cumQty + lastPxMicros * lastQty;
            this.cumQty = newCum;
            this.avgPxMicros = (int) (notional / newCum);
            this.lastTsNanos = tsNanos;
        }

        public boolean isTerminal() { return canceled || rejected || expired; }
        public boolean isActive() { return acked && !isTerminal(); }

        public ChildId childId() { return childId; }
        public VenueOrderId venueOrderId() { return venueOrderId; }
        public boolean acked() { return acked; }
        public boolean pendingCancel() { return pendingCancel; }
        public boolean canceled() { return canceled; }
        public boolean rejected() { return rejected; }
        public boolean expired() { return expired; }
        public long cumQty() { return cumQty; }
        public long avgPxMicros() { return avgPxMicros; }
        public long lastTsNanos() { return lastTsNanos; }
    }

    // ---- Cancel / Replace (parent-level) ----

    /** Caller initiated a parent cancel. Sets PENDING_CANCEL and records request time. */
    public void beginCancel(long tsNanos) {
        if (isDone()) return;
        this.cancelReqTsNanos = tsNanos;
        this.status = OrderStatus.PENDING_CANCEL;
        this.lastTsNanos = tsNanos;
    }

    /** Parent cancel confirmed (e.g., all children canceled or venue confirmed). */
    public void markCanceled(long tsNanos) {
        if (isDone()) return; // idempotent
        this.status = OrderStatus.CANCELED;
        this.doneTsNanos = tsNanos;
        this.lastTsNanos = tsNanos;
    }

    /** Begin a parent replace (resize). */
    public void beginReplace(long newOrderQty, long tsNanos) {
        if (isDone()) return;
        if (newOrderQty <= 0) throw new IllegalArgumentException("newOrderQty must be > 0");
        this.status = OrderStatus.PENDING_REPLACE;
        this.lastTsNanos = tsNanos;
    }

    /** Finalize a parent replace (ack). Updates orderQty and recomputes leaves. */
    public void applyReplaceAck(long newOrderQty, long tsNanos) {
        if (isDone()) return;
        if (newOrderQty <= 0) throw new IllegalArgumentException("newOrderQty must be > 0");
        this.orderQty = newOrderQty;
        recomputeLeavesOnly();
        if (this.orderQty <= this.cumQty) {
            this.leavesQty = 0L;
            this.status = OrderStatus.FILLED;
            if (this.firstFillTsNanos == 0L && this.cumQty > 0) this.firstFillTsNanos = tsNanos;
            this.lastFillTsNanos = tsNanos;
            this.doneTsNanos = tsNanos;
        } else {
            this.status = OrderStatus.REPLACED; // snapshot
            this.status = OrderStatus.WORKING;  // continue working
        }
        this.lastTsNanos = tsNanos;
    }

    /** Utility used after mutating orderQty to keep leaves invariant. */
    private void recomputeLeavesOnly() {
        this.leavesQty = Math.max(0L, this.orderQty - this.cumQty);
    }
}
