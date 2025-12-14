package io.tradecraft.venue.matching.orderbook;

import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.venue.model.VenueOrder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory price-time book using two priority queues + handle index. Single-writer (engine thread) expected.
 */
public final class SimpleOrderBook implements OrderBook {

    // BUY book: highest price first, then earliest time
    private final PriorityQueue<Node> bids = new PriorityQueue<>(
            Comparator.<Node>comparingLong(n -> -n.priceMicros)
                    .thenComparingLong(n -> n.timeNanos)
    );
    // SELL book: lowest price first, then earliest time
    private final PriorityQueue<Node> asks = new PriorityQueue<>(
            Comparator.<Node>comparingLong(n -> n.priceMicros)
                    .thenComparingLong(n -> n.timeNanos)
    );

    // Node storage / lookups
    private final Map<Long, Node> byNodeId = new ConcurrentHashMap<>();
    private final Map<ChildId, Node> byChildId = new ConcurrentHashMap<>();

    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public RestingRef addResting(VenueOrder vo,
                                 ChildId childId,
                                 DomainSide side,
                                 long priceMicros,
                                 long qtyLeaves,
                                 long timeNanos) {
        final long nodeId = idGen.getAndIncrement();
        final Node n = new Node(nodeId, vo, childId, side, priceMicros, qtyLeaves, timeNanos);
        pq(side).add(n);
        byNodeId.put(nodeId, n);
        byChildId.put(childId, n);
        // RestingRef carries current px + nodeId; callers should treat it as an opaque handle.
        return new RestingRef(nodeId);
    }

    @Override
    public Optional<RestingRef> byId(ChildId id) {
        final Node n = byChildId.get(id);
        return n == null ? Optional.empty() : Optional.of(toRef(n));
    }

    @Override
    public void remove(RestingRef ref) {
        final Node n = node(ref);
        if (n == null) return;
        pq(n.side).remove(n);       // O(n), acceptable for MVP; can optimize later
        byNodeId.remove(n.nodeId);
        byChildId.remove(n.childId, n);
    }

    @Override
    public Optional<RestingRef> bestContra(DomainSide takerSide) {
        final Node n = (takerSide.isBuy() ? asks.peek() : bids.peek());
        return n == null ? Optional.empty() : Optional.of(toRef(n));
    }

    @Override
    public Optional<RestingRef> popBestContra(DomainSide takerSide) {
        final Node n = (takerSide.isBuy() ? asks.poll() : bids.poll());
        if (n == null) return Optional.empty();
        byNodeId.remove(n.nodeId);
        byChildId.remove(n.childId, n);
        return Optional.of(toRef(n));
    }

    @Override
    public Iterator<RestingRef> iterateContraBestFirst(DomainSide takerSide) {
        final ArrayList<Node> snap = new ArrayList<>(takerSide.isBuy() ? asks : bids);
        // Re-apply comparator order for deterministic iteration
        if (takerSide.isBuy()) {
            snap.sort(Comparator
                    .comparingLong((Node x) -> x.priceMicros)
                    .thenComparingLong(x -> x.timeNanos));
        } else {
            snap.sort(Comparator
                    .comparingLong((Node x) -> -x.priceMicros)
                    .thenComparingLong(x -> x.timeNanos));
        }
        final Iterator<Node> it = snap.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public RestingRef next() {
                return toRef(it.next());
            }
        };
    }

    @Override
    public long availableImmediately(DomainSide takerSide, boolean isMarket, Long limitPx) {
        long acc = 0L;
        final Iterator<RestingRef> it = iterateContraBestFirst(takerSide);
        while (it.hasNext()) {
            final RestingRef r = it.next();
            if (!isMarket) {
                final boolean crossable = takerSide.isBuy()
                        ? (limitPx != null && limitPx >= priceOf(r))
                        : (limitPx != null && limitPx <= priceOf(r));
                if (!crossable) break;
            }
            acc += qtyLeavesOf(r);
        }
        return acc;
    }

    // ---------- resolvers / mutators ----------

    @Override
    public VenueOrder voOf(RestingRef ref) {
        return requireNode(ref).vo;
    }

    @Override
    public ChildId childIdOf(RestingRef ref) {
        return requireNode(ref).childId;
    }

    @Override
    public DomainSide sideOf(RestingRef ref) {
        return requireNode(ref).side;
    }

    @Override
    public long priceOf(RestingRef ref) {
        return requireNode(ref).priceMicros;
    }

    @Override
    public long qtyLeavesOf(RestingRef ref) {
        return requireNode(ref).qtyLeaves;
    }

    @Override
    public long originalQtyOf(RestingRef ref) {
        return requireNode(ref).originalQty;
    }

    @Override
    public long timeNanosOf(RestingRef ref) {
        return requireNode(ref).timeNanos;
    }

    @Override
    public void setPrice(RestingRef ref, long newPx) {
        final Node n = requireNode(ref);
        if (n.priceMicros == newPx) return;
        // Re-heap: remove, mutate, re-add
        final PriorityQueue<Node> q = pq(n.side);
        q.remove(n);
        n.priceMicros = newPx;
        q.add(n);
    }

    @Override
    public void setQtyLeaves(RestingRef ref, long newLeaves) {
        final Node n = requireNode(ref);
        n.qtyLeaves = newLeaves;
        // qty change does not affect ordering (price-time), so no re-heap needed
    }

    // ---------- internals ----------

    private PriorityQueue<Node> pq(DomainSide s) {
        return s.isBuy() ? bids : asks;
    }

    private Node node(RestingRef ref) {
        return byNodeId.get(ref.nodeId());
    }

    private Node requireNode(RestingRef ref) {
        final Node n = node(ref);
        if (n == null) throw new IllegalStateException("Stale RestingRef nodeId=" + ref.nodeId());
        return n;
    }

    private RestingRef toRef(Node n) {
        // Emit current price with handle to avoid stale px in places that log/use the ref’s px.
        return new RestingRef(n.nodeId);
    }

    private static final class Node {
        final long nodeId;
        final VenueOrder vo;
        final ChildId childId;
        final DomainSide side;
        final long timeNanos;
        long priceMicros;
        long originalQty;
        long qtyLeaves;

        Node(long nodeId,
             VenueOrder vo,
             ChildId childId,
             DomainSide side,
             long priceMicros,
             long qtyLeaves,
             long timeNanos) {
            this.nodeId = nodeId;
            this.vo = Objects.requireNonNull(vo);
            this.childId = Objects.requireNonNull(childId);
            this.side = Objects.requireNonNull(side);
            this.timeNanos = timeNanos;
            this.priceMicros = priceMicros;
            this.qtyLeaves = qtyLeaves;
            this.originalQty = qtyLeaves;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "id=" + nodeId +
                    ", side=" + side +
                    ", px=" + priceMicros +
                    ", leaves=" + qtyLeaves +
                    ", t=" + timeNanos +
                    '}';
        }
    }
}
