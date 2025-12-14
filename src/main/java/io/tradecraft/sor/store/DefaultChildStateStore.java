package io.tradecraft.sor.store;

import com.google.common.util.concurrent.Striped;
import io.tradecraft.common.id.ChildClOrdId;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.sor.core.SorEffects;
import io.tradecraft.sor.state.ChildState;
import io.tradecraft.venue.event.VenueEvent;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class DefaultChildStateStore implements ChildStateStore {
    private final ConcurrentHashMap<ChildId, ChildState> byChild = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<VenueKeyOrder, ChildId> idxByVenueOrder = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<VenueKeyClOrd, ChildId> idxByVenueClOrd = new ConcurrentHashMap<>();
    private final Striped<Lock> rowLocks = Striped.lock(2048); // or your own

    public ChildState get(ChildId id) { return byChild.get(id); }

    public ChildState upsert(ChildId id, Supplier<ChildState> create) {
        return byChild.computeIfAbsent(id, _unused -> {
            ChildState s = create.get();
            indexRouteKeys(null, s); // seed reverse index if route fields present
            return s;
        });
    }

    /** Atomic update with guard; throws if not found. */
    public ChildState update(ChildId id, UnaryOperator<ChildState> fn) {
        ChildState cur = byChild.get(id);
        if (cur == null) throw new IllegalStateException("No ChildState for " + id);
        ChildState next = Objects.requireNonNull(fn.apply(cur), "update returned null");
        byChild.put(id, next);
        indexRouteKeys(cur, next);
        return next;
    }

    @Override
    public <T extends PubChildIntent> SorEffects apply(
            ChildId id, T intent, BiFunction<ChildState, T, SorEffects> reducer) {

        Lock l = rowLocks.get(id);
        l.lock();
        try {
            ChildState cur = byChild.get(id);
            if (cur == null) return SorEffects.noop(); // or throw

            SorEffects eff = reducer.apply(cur, intent);
            ChildState next = Objects.requireNonNull(eff.next(), "reducer returned null next state");
            byChild.put(id, next);
            indexIfMappedChanged(cur, next);
            return eff;
        } finally {
            l.unlock();
        }
    }

    @Override
    public <T extends VenueEvent> SorEffects apply(
            VenueId venueId,
            @Nullable VenueOrderId venueOrderId,
            @Nullable ChildClOrdId childClOrdId,
            T venueEvent,
            BiFunction<ChildState, T, SorEffects> reducer) {

        // Resolve child id via reverse indexes
        ChildId id = null;
        if (venueOrderId != null) {
            id = idxByVenueOrder.get(new VenueKeyOrder(venueId, venueOrderId));
        }
        if (id == null && childClOrdId != null) {
            id = idxByVenueClOrd.get(new VenueKeyClOrd(venueId, childClOrdId));
        }
        if (id == null) {
            // Unknown mapping — out-of-order venue event. Your policy:
            // return NOOP, buffer, or dead-letter.
            return SorEffects.noop();
        }

        Lock l = rowLocks.get(id);
        l.lock();
        try {
            ChildState cur = byChild.get(id);
            if (cur == null) return SorEffects.noop(); // row vanished/terminal-cleanup

            SorEffects eff = reducer.apply(cur, venueEvent);
            ChildState next = Objects.requireNonNull(eff.next(), "reducer returned null next state");
            byChild.put(id, next);
            indexIfMappedChanged(cur, next);
            return eff;
        } finally {
            l.unlock();
        }
    }

    // ------------ reverse index maintenance ------------
    private void indexRouteKeys(ChildState prev, ChildState next) {
        // (venueId, childClOrdId) should be present before sending to venue (seeded at New)
        if (next.venueId() != null && next.childClOrdId() != null) {
            idxByVenueClOrd.put(new VenueKeyClOrd(next.venueId(), next.childClOrdId()), next.childId());
        }

        // (venueId, venueOrderId) becomes known on VenueAck or later
        if (next.venueId() != null && next.venueOrderId() != null) {
            idxByVenueOrder.put(new VenueKeyOrder(next.venueId(), next.venueOrderId()), next.childId());
        }

        // If route keys changed (rare), you could remove old keys here:
        // remove old idx when venueId/childClOrdId/venueOrderId changed
        if (prev != null && !Objects.equals(prev.venueId(), next.venueId())) {
            // venue switched (shouldn’t happen in practice for a child)
            if (prev.venueId() != null && prev.childClOrdId() != null)
                idxByVenueClOrd.remove(new VenueKeyClOrd(prev.venueId(), prev.childClOrdId()));
            if (prev.venueId() != null && prev.venueOrderId() != null)
                idxByVenueOrder.remove(new VenueKeyOrder(prev.venueId(), prev.venueOrderId()));
        } else if (prev != null) {
            if (!Objects.equals(prev.childClOrdId(), next.childClOrdId()) && prev.childClOrdId() != null && prev.venueId() != null)
                idxByVenueClOrd.remove(new VenueKeyClOrd(prev.venueId(), prev.childClOrdId()));
            if (!Objects.equals(prev.venueOrderId(), next.venueOrderId()) && prev.venueOrderId() != null && prev.venueId() != null)
                idxByVenueOrder.remove(new VenueKeyOrder(prev.venueId(), prev.venueOrderId()));
        }
    }

    private void indexIfMappedChanged(ChildState prev, ChildState next) {
        // Promote indices when fields appear/change
        if (!Objects.equals(prev.venueId(), next.venueId())
                || !Objects.equals(prev.venueOrderId(), next.venueOrderId())) {
            if (prev.venueId() != null && prev.venueOrderId() != null) {
                idxByVenueOrder.remove(new VenueKeyOrder(prev.venueId(), prev.venueOrderId()));
            }
            if (next.venueId() != null && next.venueOrderId() != null) {
                idxByVenueOrder.put(new VenueKeyOrder(next.venueId(), next.venueOrderId()), next.childId());
            }
        }

        if (!Objects.equals(prev.venueId(), next.venueId())
                || !Objects.equals(prev.childClOrdId(), next.childClOrdId())) {
            if (prev.venueId() != null && prev.childClOrdId() != null) {
                idxByVenueClOrd.remove(new VenueKeyClOrd(prev.venueId(), prev.childClOrdId()));
            }
            if (next.venueId() != null && next.childClOrdId() != null) {
                idxByVenueClOrd.put(new VenueKeyClOrd(next.venueId(), next.childClOrdId()), next.childId());
            }
        }
    }

    private record VenueKeyOrder(VenueId v, VenueOrderId o) {}
    private record VenueKeyClOrd(VenueId v, ChildClOrdId c) {}
}

