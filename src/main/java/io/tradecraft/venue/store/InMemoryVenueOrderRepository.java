package io.tradecraft.venue.store;

import io.tradecraft.common.domain.order.CancelReason;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.matching.orderbook.FillSource;
import io.tradecraft.venue.matching.orderbook.RestingRef;
import io.tradecraft.venue.model.VenueOrder;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory, writer-friendly repository for venueId orders.
 * <p>
 * Responsibilities: - Store/lookup VenueOrder by ChildId and reverse-lookup by VenueOrderId - Apply simple lifecycle
 * transitions (ack/fill/replace/cancel) - Provide lightweight idempotency guards (seenCmd/seenExec)
 * <p>
 * Not responsible for: - Generating VenueOrderId (callers must provide it)
 */
public final class InMemoryVenueOrderRepository implements VenueOrderRepository {

    private final ConcurrentMap<ChildId, VenueOrder> byChild = new ConcurrentHashMap<>();
    private final ConcurrentMap<VenueOrderId, ChildId> byVenue = new ConcurrentHashMap<>();

    // venueId-scope idempotency guards
    private final ConcurrentMap<String, Boolean> seenCmds = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> seenExecs = new ConcurrentHashMap<>();

    private final DualTimeSource dualTimeSource;

    public InMemoryVenueOrderRepository(DualTimeSource dualTimeSource) {
        this.dualTimeSource = dualTimeSource;
    }

    // ----------------------------------------------------------------------
    // Creation & lookups
    // ----------------------------------------------------------------------

    /**
     * Creates a VenueOrder for the given child IF absent (idempotent on ChildId). The VenueOrderId must be supplied by
     * the caller (usually via VenueSupport's id factory).
     */
    @Override
    public VenueOrder create(NewChildCmd cmd, VenueId venueId, VenueOrderId venueOrderId) {
        return byChild.compute(cmd.childId(), (childKey, existing) -> {
            if (existing != null) return existing;

            final VenueOrder vo = VenueOrder.from(cmd, venueId, venueOrderId, dualTimeSource.nowNanos());
            // populate reverse index (best-effort; tolerate races)
            byVenue.putIfAbsent(venueOrderId, cmd.childId());
            return vo;
        });
    }

    @Override
    public Optional<VenueOrder> get(ChildId childId) {
        return Optional.ofNullable(byChild.get(childId));
    }

    @Override
    public Optional<VenueOrder> byVenue(VenueOrderId venueOrderId) {
        final ChildId childId = byVenue.get(venueOrderId);
        return (childId == null) ? Optional.empty() : Optional.ofNullable(byChild.get(childId));
    }

    // ----------------------------------------------------------------------
    // Lifecycle transitions
    // ----------------------------------------------------------------------

    @Override
    public void ack(VenueOrder vo, long tsNanos) {
        // If you carry status/ts in VenueOrder, copy/update here
        byChild.computeIfPresent(vo.childId(), (k, cur) -> cur); // no-op default
    }

    @Override
    public void applyFill(VenueOrder vo,
                          long lastQty,
                          long lastPxMicros,
                          boolean finalFlag,
                          FillSource src) {
        byChild.computeIfPresent(vo.childId(), (k, cur) -> {
            final long newCum = Math.addExact(cur.cumQty(), lastQty);
            final long newLeaves = Math.max(0L, cur.qty() - newCum);

            // If VenueOrder tracks avgPx/lastPx, extend VenueOrder and compute here.
            return new VenueOrder(
                    cur.parentId(), cur.childId(), cur.childClOrdId(),
                    cur.venueId(), cur.venueOrderId(),
                    cur.side(), cur.qty(), cur.ordType(),
                    cur.limitPxMicros(),
                    newCum,
                    newLeaves,
                    cur.entryTsNanos()
            );
        });
    }

    @Override
    public void markResting(VenueOrder vo, RestingRef ref) {
        // If you add explicit resting state, copy it here.
        byChild.computeIfPresent(vo.childId(), (k, cur) -> cur);
    }

    @Override
    public void clearResting(VenueOrder vo) {
        byChild.computeIfPresent(vo.childId(), (k, cur) -> cur);
    }

    @Override
    public void cancel(VenueOrder vo, CancelReason reason) {
        // Remove primary row and reverse mapping
        final ChildId childId = vo.childId();
        final VenueOrder removed = byChild.remove(childId);
        if (removed != null && removed.venueOrderId() != null) {
            byVenue.remove(removed.venueOrderId(), childId);
        }
    }

    @Override
    public void applyReplace(VenueOrder vo, long newQty, @Nullable Long newLimitPxMicros) {
        byChild.computeIfPresent(vo.childId(), (k, cur) -> {
            final long boundedLeaves = Math.max(0L, newQty - cur.cumQty());
            final Long limit = (newLimitPxMicros != null ? newLimitPxMicros : cur.limitPxMicros());
            return new VenueOrder(
                    cur.parentId(), cur.childId(), cur.childClOrdId(),
                    cur.venueId(), cur.venueOrderId(),
                    cur.side(), newQty, cur.ordType(),
                    limit,
                    cur.cumQty(),
                    boundedLeaves,
                    cur.entryTsNanos()
            );
        });
    }

    // ----------------------------------------------------------------------
    // Idempotency (venueId-level truth)
    // ----------------------------------------------------------------------

    @Override
    public boolean seenCmd(String cmdId) {
        // returns true on first sight, false if already seen
        return seenCmds.putIfAbsent(cmdId, Boolean.TRUE) == null;
    }

    @Override
    public boolean seenExec(String execId) {
        // returns true on first sight, false if already seen
        return seenExecs.putIfAbsent(execId, Boolean.TRUE) == null;
    }
}
