package io.tradecraft.venue.matching;

import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.domain.order.CancelReason;
import io.tradecraft.venue.api.VenueExecution;
import io.tradecraft.venue.api.VenueSupport;
import io.tradecraft.venue.cmd.CancelChildCmd;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.cmd.ReplaceChildCmd;
import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueCancelDone;
import io.tradecraft.venue.event.VenueFill;
import io.tradecraft.venue.matching.orderbook.FillSource;
import io.tradecraft.venue.matching.orderbook.OrderBook;
import io.tradecraft.venue.matching.orderbook.RestingRef;
import io.tradecraft.venue.matching.orderbook.SimpleOrderBook;
import io.tradecraft.venue.model.VenueOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Single-venueId matching engine.
 * <p>
 * Responsibilities: - Policy & orchestration (match, rest, IOC/FOK, replace) - Data structure delegation to OrderBook
 * (opaque RestingRef handle) - Side-effects delegated to VenueSupport (create/ack/fill/cancel/clearResting)
 * <p>
 * This class is single-threaded (engine thread). If you adopt a different thread model, make sure your OrderBook
 * implementation provides the needed concurrency guarantees.
 */
public final class MatchingEngine {

    private final VenueSupport support;
    private final OrderBook book;

    /**
     * Default constructor uses a SimpleOrderBook.
     */
    public MatchingEngine(VenueSupport support) {
        this(support, new SimpleOrderBook());
    }

    public MatchingEngine(VenueSupport support, OrderBook book) {
        this.support = Objects.requireNonNull(support, "support");
        this.book = Objects.requireNonNull(book, "book");
    }

    // -----------------------------------------------------------------------
    // Command entry points (called by MatchingEngineStrategy)
    // -----------------------------------------------------------------------

    /**
     * Handle NewChildCmd: cross if marketable; rest remainder if LIMIT and allowed by TIF.
     */
    public VenueExecution onNew(NewChildCmd c) {
        final List<VenueAck> acks = new ArrayList<>(1);
        final List<VenueFill> fills = new ArrayList<>(4);
        VenueCancelDone cancel = null;

        // Create VO and ACK immediately
        final VenueOrder vo = support.create(c);
        acks.add(support.ack(c, vo));

        final DomainSide side = c.side();
        final DomainOrdType ordType = c.ordType();
        final DomainTif tif = c.tif();

        final boolean isMarket = ordType.isMarket();
        final boolean isLimit = ordType.isLimit();
        final boolean ioc = tif == DomainTif.IOC;
        final boolean fok = tif == DomainTif.FOK;

        final Long limitPx = c.priceMicros(); // may be null for MARKET
        long leaves = c.qty();

        // FOK: must be fully fillable immediately
        if (fok && book.availableImmediately(side, isMarket, limitPx) < leaves) {
            cancel = support.cancel(vo, CancelReason.UNFILLED);
            return VenueExecution.of(acks, fills, cancel, null);
        }

        // Match loop (taker)
        while (leaves > 0) {
            var bestOpt = book.bestContra(side);
            if (bestOpt.isEmpty()) break;
            final RestingRef contra = bestOpt.get();

            // --- Resolve everything BEFORE mutating the book ---
            final long contraPx;
            final long contraLeaves;
            final VenueOrder resting;
            try {
                contraPx = book.priceOf(contra);
                contraLeaves = book.qtyLeavesOf(contra);
                resting = book.voOf(contra);
            } catch (IllegalStateException stale) {
                // Node invalidated between bestContra() and reads; retry loop to fetch new best
                continue;
            }

            // Marketability check against resolved price
            final boolean marketable = isMarket
                    || (isLimit && (side.isBuy()
                    ? (limitPx != null && limitPx >= contraPx)
                    : (limitPx != null && limitPx <= contraPx)));
            if (!marketable) break;

            final long execQty = Math.min(leaves, contraLeaves);

            // Emit taker fill (add maker-side fill as well if your model requires it)
            fills.add(support.applyFill(
                    vo, execQty, contraPx,
                    /* terminal */ false, FillSource.MATCHING_ENGINE));

            // 2) maker fill (resting contra order)
            final VenueOrder venueOrder = book.voOf(contra);
            final boolean makerDone = venueOrder.leavesQty() - contraLeaves > 0;
            fills.add(support.applyFill(
                    venueOrder, execQty, contraPx, makerDone , FillSource.MATCHING_ENGINE));

            // Update both sides' leaves locally
            leaves -= execQty;
            final long contraNewLeaves = contraLeaves - execQty;

            // --- Mutate AFTER we’re done using the ref ---
            if (contraNewLeaves > 0) {
                book.setQtyLeaves(contra, contraNewLeaves);
            } else {
                // Remove current best; DO NOT dereference `contra` after this point
                book.popBestContra(side);
                // We resolved `resting` earlier, so it's safe to pass now
                support.clearResting(resting);
            }
            // loop: always re-fetch bestContra() next iteration
        }

        // Post-matching: rest or cancel remainder
        if (leaves > 0) {
            if (ioc || isMarket) {
                cancel = support.cancel(vo, CancelReason.UNFILLED);
            } else if (isLimit) {
                // Use venueId DualTimeSource for determinism
                book.addResting(vo, c.childId(), side, limitPx, leaves, support.dualTimeSource().nowNanos());
            }
        }

        return VenueExecution.of(acks, fills, cancel, null);
    }

    /**
     * Handle CancelChildCmd: remove resting line if present; emit cancel.
     */
    public VenueExecution onCancel(CancelChildCmd x) {
        final var acks = List.<VenueAck>of();
        final var fills = List.<VenueFill>of();

        var refOpt = book.byId(x.childId());
        if (refOpt.isEmpty()) return VenueExecution.noop();

        var ref = refOpt.get();

        // Resolve BEFORE mutating the book
        final VenueOrder resting;
        final long leaves;
        try {
            resting = book.voOf(ref);
            leaves = book.qtyLeavesOf(ref);
        } catch (IllegalStateException stale) {
            // node vanished; nothing left to cancel
            return VenueExecution.noop();
        }
        if (leaves <= 0) return VenueExecution.noop();

        // Order matters: zero out leaves, then remove node
        book.setQtyLeaves(ref, 0L);   // <-- test verifies this call
        book.remove(ref);

        // Use cached VO only; ref is invalid now
        support.clearResting(resting);

        // Emit cancel with canceledQty present
        var cancel = support.cancel(resting, leaves, CancelReason.USER_REQUEST);

        return VenueExecution.of(acks, fills, cancel, null);
    }


    /**
     * Handle ReplaceChildCmd: reprice/re-size a resting order. Policy: - Remove from book, mutate price/qty - Try to
     * cross as maker at new price - Re-rest remainder (same time priority policy as your venueId; typically new time)
     */
    public VenueExecution onReplace(ReplaceChildCmd r) {
        final List<VenueAck> acks = new ArrayList<>(1);
        final List<VenueFill> fills = new ArrayList<>(4);
        VenueCancelDone cancel = null;

        final Optional<RestingRef> refOpt = book.byId(r.childId());
        if (refOpt.isEmpty()) {
            return VenueExecution.noop(); // Nothing to replace (or too late)
        }

        // Remove from book while mutating/matching
        final RestingRef ref = refOpt.get();
        final var side = book.sideOf(ref);
        final var vo = book.voOf(ref);
        book.remove(ref);

        // Apply new attributes
        if (r.newLimitPxMicros() != null) {
            book.setPrice(ref, r.newLimitPxMicros());
        }
        if (r.newQty() != null) {
            final long alreadyExec = book.originalQtyOf(ref) - book.qtyLeavesOf(ref);
            final long newLeaves = Math.max(0L, r.newQty() - alreadyExec);
            book.setQtyLeaves(ref, newLeaves);
        }

        // Attempt to cross as maker at updated price
        while (book.qtyLeavesOf(ref) > 0) {
            final Optional<RestingRef> bestOpt = book.bestContra(side);
            if (bestOpt.isEmpty()) break;
            final RestingRef contra = bestOpt.get();

            final long makerPx = book.priceOf(ref);
            final long contraPx = book.priceOf(contra);

            final boolean crossable = side.isBuy() ? (makerPx >= contraPx)
                    : (makerPx <= contraPx);
            if (!crossable) break;

            final long makerLeaves = book.qtyLeavesOf(ref);
            final long contraLeaves = book.qtyLeavesOf(contra);
            final long execQty = Math.min(makerLeaves, contraLeaves);

            fills.add(support.applyFill(vo, execQty, contraPx, false, FillSource.MATCHING_ENGINE));

            book.setQtyLeaves(ref, makerLeaves - execQty);
            book.setQtyLeaves(contra, contraLeaves - execQty);

            if (book.qtyLeavesOf(contra) == 0) {
                book.popBestContra(side);
                support.clearResting(book.voOf(contra));
            }
        }

        // Re-rest remainder at new terms; else order is done
        if (book.qtyLeavesOf(ref) > 0) {
            book.addResting(vo, book.childIdOf(ref), side, book.priceOf(ref),
                    book.qtyLeavesOf(ref), support.dualTimeSource().nowNanos());
        } else {
            support.clearResting(vo);
        }

        // If you emit a "replace-ack" event, add it here via support.* and include in 'acks'
        return VenueExecution.of(acks, fills, cancel, null);
    }

    // -----------------------------------------------------------------------
    // (Optional) Utilities you might expose in the future (e.g., query book)
    // -----------------------------------------------------------------------

    /**
     * Returns immediate available quantity for a taker side/price (useful for diagnostics).
     */
    public long availableImmediately(DomainSide side, boolean isMarket, Long limitPx) {
        return book.availableImmediately(side, isMarket, limitPx);
    }
}
