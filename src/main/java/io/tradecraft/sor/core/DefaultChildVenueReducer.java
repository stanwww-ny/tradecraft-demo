package io.tradecraft.sor.core;

import io.tradecraft.common.domain.order.CancelReason;
import io.tradecraft.common.domain.order.CancelRejectReason;
import io.tradecraft.common.domain.order.RejectReason;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.oms.event.EvChildAck;
import io.tradecraft.oms.event.EvChildCancelReject;
import io.tradecraft.oms.event.EvChildCanceled;
import io.tradecraft.oms.event.EvChildPendingCancel;
import io.tradecraft.oms.event.EvChildReject;
import io.tradecraft.oms.event.EvChildReplaceReject;
import io.tradecraft.oms.event.EvChildReplaced;
import io.tradecraft.oms.event.EvFill;
import io.tradecraft.sor.state.ChildState;
import io.tradecraft.sor.state.ChildStatus;
import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueCancelAck;
import io.tradecraft.venue.event.VenueCancelDone;
import io.tradecraft.venue.event.VenueCancelReject;
import io.tradecraft.venue.event.VenueEvent;
import io.tradecraft.venue.event.VenueFill;
import io.tradecraft.venue.event.VenueNewReject;
import io.tradecraft.venue.event.VenueReplaceAck;
import io.tradecraft.venue.event.VenueReplaceReject;

import java.util.List;

public class DefaultChildVenueReducer implements ChildVenueReducer{
    public DefaultChildVenueReducer() {}
    @Override
    public SorEffects reduce(ChildState s, VenueEvent ev) {
        // 1) Terminal guard: never mutate after FILLED / CANCELED / REJECTED
        if (s.status().isTerminal()) return SorEffects.of(s);

        // 2) Stale/mismatch guard (optional but recommended)
        if (isStaleOrMismatched(s, ev)) return SorEffects.of(s);

        return switch (ev) {

            // ---- NEW / ACK / REJECT -----------------------------------------------------
            case VenueAck a                -> onNewAck(s, a);        // NEW_PENDING -> ACKED (+map venueOrderId, emit ACK ER)
            case VenueNewReject r          -> onNewReject(s, r);     // NEW_PENDING -> REJECTED (terminal, emit REJECT ER)

            // ---- TRADES / FILLS ---------------------------------------------------------
            case VenueFill f               -> onFill(s, f);          // ACKED/PF/PCAN -> update cum/leaves; -> FILLED if leaves==0 (emit TRADE ER)

            // ---- CANCEL PATH ------------------------------------------------------------
            // (Some venues send an intermediate "acknowledged" before the final done.)
            case VenueCancelAck a          -> onCancelAck(s, a);     // Typically stay PENDING_CANCEL; may be a no-op + ER PendingCancel
            case VenueCancelDone d         -> onCancelDone(s, d);    // PENDING_CANCEL -> CANCELED (terminal, emit CANCELED ER)
            case VenueCancelReject r       -> onCancelReject(s, r);  // PENDING_CANCEL -> back to ACKED/PF, emit CancelReject ER

            // ---- REPLACE PATH -----------------------------------------------------------
            case VenueReplaceAck ra        -> onReplaceAck(s, ra);   // stay ACKED (or clear PENDING_REPLACE), update px/leaves, emit ReplaceAck ER
            case VenueReplaceReject rr     -> onReplaceReject(s, rr);// rollback transient, -> ACKED, emit ReplaceReject ER

            // ---- DEFAULT ----------------------------------------------------------------
            default                        -> SorEffects.of(s);       // explicit no-op for anything else
        };
    }

    // In DefaultChildVenueReducer (pure reducer)

    private SorEffects onNewReject(ChildState s, VenueNewReject ev) {
        // Only NEW_PENDING should move to REJECTED
        if (s.status() != ChildStatus.NEW_PENDING) return SorEffects.of(s);

        var next = s.toBuilder()
                .status(ChildStatus.REJECTED)
                .updatedTsNanos(ev.tsNanos())
                .build();

        // Optional ER: REJECT
        var er =  EvChildReject.builder()
                .childId(s.childId())
                .parentId(s.parentId())
                .venueId(ev.venueId())
                .venueOrderId(ev.venueOrderId())       // may be null
                .childClOrdId(s.childClOrdId())
                .rejectReason(ev.reason())
                .text(ev.text())
                .tsNanos(ev.tsNanos())
                .build();

        return SorEffects.builder().next(next).orderEvents(List.of(er)).build();
    }

    // Inside DefaultChildVenueReducer (pure reducer, no IO)

    private SorEffects onNewAck(ChildState s, VenueAck v) {
        // Only NEW_PENDING should become ACKED
        if (s.status() != ChildStatus.NEW_PENDING) return SorEffects.of(s);

        var next = s.toBuilder()
                .venueId(v.venueId() != null ? v.venueId() : s.venueId())
                .venueOrderId(v.venueOrderId())
                .status(ChildStatus.ACKED)
                .updatedTsNanos(v.tsNanos())
                .build();

        var er = EvChildAck.builder()
                .parentId(s.parentId())
                .childId(s.childId())
                .childClOrdId(s.childClOrdId())
                .venueId(v.venueId() != null ? v.venueId() : s.venueId())
                .venueOrderId(v.venueOrderId())
                .execId(v.execId())
                .tsNanos(v.tsNanos())
                .build(); // optional; return null if not emitting ERs

        return SorEffects.builder().next(next).orderEvents(List.of(er)).build();
    }

    private SorEffects onFill(ChildState s, VenueFill v) {
        // Allowed when working or cancel-pending
        if (!(s.status() == ChildStatus.ACKED
                || s.status() == ChildStatus.PARTIALLY_FILLED
                || s.status() == ChildStatus.PENDING_CANCEL)) {
            return SorEffects.of(s);
        }

        // Optional idempotency by execId (if you have one on the event)
        if (isDuplicateTrade(s, v)) return SorEffects.of(s);

        long lastQty   = v.lastQty();
        long newCum    = Math.addExact(s.cumQty(), lastQty);
        long newLeaves = Math.max(0, s.qty() - newCum);

        // Determine status:
        final ChildStatus newStatus;
        if (newLeaves == 0) {
            newStatus = ChildStatus.FILLED;        // fill beats cancel if completed
        } else if (s.status() == ChildStatus.PENDING_CANCEL) {
            newStatus = ChildStatus.PENDING_CANCEL; // still cancel-pending with residual leaves
        } else {
            newStatus = ChildStatus.PARTIALLY_FILLED;
        }

        var next = s.toBuilder()
                .cumQty(newCum)
                .leavesQty(newLeaves)
                .status(newStatus)
                .updatedTsNanos(v.tsNanos())
                .build();

        var er = EvFill.builder()
                .childId(s.childId())
                .parentId(s.parentId())
                .venueId(v.venueId())
                .venueOrderId(v.venueOrderId())
                .childClOrdId(s.childClOrdId())
                .execId(v.execId())                    // if your VenueFill has it; else remove
                .lastQty(v.lastQty())
                .lastPxMicros(v.lastPxMicros())// optional if present
                .cumQty(v.cumQty())
                .tsNanos(v.tsNanos())
                .build();

        return SorEffects.builder().next(next).orderEvents(List.of(er)).build();
    }

    private SorEffects onCancelAck(ChildState s, VenueCancelAck ev) {
        // Usually just confirms pending cancel; keep PENDING_CANCEL
        if (s.status() != ChildStatus.PENDING_CANCEL) return SorEffects.of(s);

        var next = s.toBuilder()
                .updatedTsNanos(ev.tsNanos())
                .build();

        var er = EvChildPendingCancel.builder()
                .childId(s.childId())
                .parentId(s.parentId())
                .venueId(ev.venueId())
                .venueOrderId(ev.venueOrderId())
                .childClOrdId(s.childClOrdId())
                .tsNanos(ev.tsNanos())
                .build(); // optional: emit ER(PENDING_CANCEL)

        return SorEffects.builder().next(next).orderEvents(List.of(er)).build();
    }

    private SorEffects onCancelDone(ChildState s, VenueCancelDone ev) {
        if (s.status() != ChildStatus.PENDING_CANCEL) return SorEffects.of(s);

        var next = s.toBuilder()
                .status(ChildStatus.CANCELED)
                .updatedTsNanos(ev.tsNanos())
                .build();

        var er =  EvChildCanceled.builder()
                .childId(s.childId())
                .parentId(s.parentId())
                .venueId(ev.venueId())
                .venueOrderId(ev.venueOrderId())
                .childClOrdId(s.childClOrdId())
                .cancelReason(CancelReason.USER_REQUEST)      // or VENUE/EXTERNAL if you track; adjust as needed
                .tsNanos(ev.tsNanos())
                .build(); // optional
        return SorEffects.builder().next(next).orderEvents(List.of(er)).build();
    }

    private SorEffects onCancelReject(ChildState s, VenueCancelReject ev) {
        if (s.status() != ChildStatus.PENDING_CANCEL) return SorEffects.of(s);

        // Roll back to working state depending on cumQty
        var rolled = (s.cumQty() > 0) ? ChildStatus.PARTIALLY_FILLED : ChildStatus.ACKED;

        var next = s.toBuilder()
                .status(rolled)
                .updatedTsNanos(ev.tsNanos())
                .build();

        var er = EvChildCancelReject.builder()
                .childId(s.childId())
                .parentId(s.parentId())
                .venueId(ev.venueId())
                .venueOrderId(ev.venueOrderId())
                .childClOrdId(s.childClOrdId())
                .reason(ev.reason())
                .text(ev.text())
                .tsNanos(ev.tsNanos())
                .build();

        return SorEffects.builder().next(next).orderEvents(List.of(er)).build();
    }


    private SorEffects onReplaceAck(ChildState s, VenueReplaceAck ev) {
        // Replace ack is valid while working (ACKED/PARTIALLY_FILLED) or during transient replace
        if (!(s.status() == ChildStatus.ACKED ||
                s.status() == ChildStatus.PARTIALLY_FILLED ||
                s.status() == ChildStatus.PENDING_REPLACE)) {
            return SorEffects.of(s);
        }

        var b = s.toBuilder()
                .updatedTsNanos(ev.tsNanos());

        if (ev.newPriceMicros() != null) {
            b.lastPxMicros(ev.newPriceMicros());
        }
        if (ev.newLeavesQty() != null) {
            // Keep cumQty unchanged; only venue-adjusted leaves is accepted here
            b.leavesQty(ev.newLeavesQty());
        }

        // Clear transient replace state if you model it
        var next = b
                .status(s.cumQty() > 0 ? ChildStatus.PARTIALLY_FILLED : ChildStatus.ACKED)
                .build();

        // Optional ER: REPLACE_ACK (aka OrderReplaceAccepted / OrderRestated)
        var er = EvChildReplaced.builder()
                .childId(s.childId())
                .parentId(s.parentId())
                .venueId(ev.venueId())
                .venueOrderId(ev.venueOrderId())
                .childClOrdId(s.childClOrdId())
                .newPriceMicros(ev.newPriceMicros())    // may be null
                .newLeavesQty(ev.newLeavesQty())        // may be null
                .tsNanos(ev.tsNanos())
                .build();

        return SorEffects.builder().next(next).orderEvents(List.of(er)).build();    }

    private SorEffects onReplaceReject(ChildState s, VenueReplaceReject ev) {
        // Replace reject usually arrives when we’re working or transient-replacing
        if (!(s.status() == ChildStatus.ACKED ||
                s.status() == ChildStatus.PARTIALLY_FILLED ||
                s.status() == ChildStatus.PENDING_REPLACE)) {
            return SorEffects.of(s);
        }

        // Roll back any transient local flag; keep price/leaves as-is
        var next = s.toBuilder()
                .updatedTsNanos(ev.tsNanos())
                .status(s.cumQty() > 0 ? ChildStatus.PARTIALLY_FILLED : ChildStatus.ACKED)
                .build();

        // Optional ER: REPLACE_REJECT
        var er = EvChildReplaceReject.builder()
                .childId(s.childId())
                .parentId(s.parentId())
                .venueId(ev.venueId())
                .venueOrderId(ev.venueOrderId())
                .childClOrdId(s.childClOrdId())
                .replaceRejectReason(ev.reason())
                .tsNanos(ev.tsNanos())
                .build();
        return SorEffects.builder().next(next).orderEvents(List.of(er)).build();
    }


    private boolean isStaleOrMismatched(ChildState s, VenueEvent ev) {
        // Pseudocode; adapt to your getters being nullable
        if (ev.venueId() != null && s.venueId() != null && !ev.venueId().equals(s.venueId())) return true;
        if (ev.childClOrdId() != null && !ev.childClOrdId().equals(s.childClOrdId())) return true;
        return ev.venueOrderId() != null && s.venueOrderId() != null && !ev.venueOrderId().equals(s.venueOrderId());
    }

    private boolean isDuplicateTrade(ChildState s, VenueFill ev) {
        // e.g., return s.appliedExecIds().contains(ev.execId());
        return false;
    }

    private PubExecReport erAck(ChildState st, long tsNanos) { return null; }
    private PubExecReport erTrade(ChildState st, VenueFill f, long tsNanos) { return null; }
    private PubExecReport erPendingCancel(ChildState st, long tsNanos) { return null; }
    private PubExecReport erCanceled(ChildState st, long tsNanos) { return null; }
    private PubExecReport erCancelReject(ChildState st, CancelRejectReason reason, String text, long tsNanos) { return null; }
    // Return null if you haven’t wired ERs yet.
    private PubExecReport erReject(ChildState next, RejectReason reason, String text, long tsNanos) {
        // Example factory; adjust to your ctor/factory:
        // return PubExecReport.reject(next.parentId(), next.childId(), next.childClOrdId(), rejectReason, text, tsNanos);
        return null;
    }

    private PubExecReport erReplaceAck(ChildState next, long tsNanos) {
        // return PubExecReport.replaceAck(next.parentId(), next.childId(), next.childClOrdId(), next.priceMicros(), next.leavesQty(), tsNanos);
        return null;
    }

    private PubExecReport erReplaceReject(ChildState next, RejectReason reason, String text, long tsNanos) {
        // return PubExecReport.replaceReject(next.parentId(), next.childId(), next.childClOrdId(), rejectReason, text, tsNanos);
        return null;
    }

}
