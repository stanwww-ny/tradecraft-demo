package io.tradecraft.venue.matching.orderbook;

import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.venue.model.VenueOrder;

import java.util.Iterator;
import java.util.Optional;

/**
 * Price-time order book interface (no side-effects). Implementations must be thread-confined to the engine thread or
 * provide their own concurrency model.
 * <p>
 * The handle returned by the book is a RestingRef — a stable reference to a resting order line. MatchingEngine uses it
 * to read/update qty/price and to identify the order for cancels.
 */
public interface OrderBook {

    /**
     * Add a new resting order line and return a handle to it.
     */
    RestingRef addResting(VenueOrder vo,
                          ChildId childId,
                          DomainSide side,
                          long priceMicros,
                          long qtyLeaves,
                          long timeNanos);

    /**
     * Lookup a resting line by child id.
     */
    Optional<RestingRef> byId(ChildId id);

    /**
     * Remove a resting line from the book. Idempotent if already absent.
     */
    void remove(RestingRef ref);

    /**
     * Peek the best contra for a taker side (do not remove).
     */
    Optional<RestingRef> bestContra(DomainSide takerSide);

    /**
     * Pop the best contra for a taker side (remove).
     */
    Optional<RestingRef> popBestContra(DomainSide takerSide);

    /**
     * Iterate contra side from best to worse (snapshot ordering).
     */
    Iterator<RestingRef> iterateContraBestFirst(DomainSide takerSide);

    /**
     * Total immediately available quantity for a taker given market/limit. If isMarket=true, sums all contra; otherwise
     * only sums crossable prices.
     */
    long availableImmediately(DomainSide takerSide, boolean isMarket, Long limitPx);

    // Resolvers / mutators (single-writer engine thread):
    VenueOrder voOf(RestingRef ref);

    ChildId childIdOf(RestingRef ref);

    DomainSide sideOf(RestingRef ref);

    long priceOf(RestingRef ref);

    long qtyLeavesOf(RestingRef ref);

    long originalQtyOf(RestingRef ref);

    long timeNanosOf(RestingRef ref);

    void setPrice(RestingRef ref, long newPx);

    void setQtyLeaves(RestingRef ref, long newLeaves);
}
