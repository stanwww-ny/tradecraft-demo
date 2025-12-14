package io.tradecraft.venue.matching.orderbook;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.venue.model.VenueOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies: - bestContra(side) returns top-of-book on the contra side - qtyLeavesOf / setQtyLeaves mutate leaves
 * correctly - popBestContra(side) removes the top entry when leaves==0 - availableImmediately reflects each mutation
 * <p>
 * Wire the two helpers (restSell/restBuy) to your SimpleOrderBook add/rest API, then remove @Disabled.
 */
class SimpleOrderBookContraOperationsTest {

    private final DualTimeSource dualTimeSource = TestClocks.msTicker();
    OrderBook book;

    @BeforeEach
    void setUp() {
        book = new SimpleOrderBook();

        // SELL book (contra to BUY taker): best ask = 100.50 × 40, then 101.00 × 60
        restSell(book, 100_50L, 40L);
        restSell(book, 101_00L, 60L);

        // BUY book (contra to SELL taker): best bid = 99.50 × 50, then 99.00 × 70
        restBuy(book, 99_50L, 50L);
        restBuy(book, 99_00L, 70L);
    }

    @Test
    @DisplayName("BUY taker: bestContra→partial→pop→next level; availableImmediately updates each step")
    void buyTaker_contraFlow() {
        // Initial availability (BUY market consumes all SELL depth)
        assertEquals(100L, book.availableImmediately(DomainSide.BUY, true, null));

        // Best contra is lowest ask (100.50)
        Optional<RestingRef> maybeContra = book.bestContra(DomainSide.BUY);
        assertTrue(maybeContra.isPresent(), "best contra should be present for BUY taker");
        RestingRef contra = maybeContra.get();
        assertNotNull(contra, "best contra must exist");
        long leaves = book.qtyLeavesOf(contra);
        assertEquals(40L, leaves, "top ask leaves must be 40 initially");
        assertNotNull(book.voOf(contra), "voOf should return the backing VenueOrder");

        // Partial fill 10 on the best ask
        long execQty = 10L;
        book.setQtyLeaves(contra, leaves - execQty);
        assertEquals(30L, book.qtyLeavesOf(contra), "leaves reduce by executed qty");
        assertEquals(90L, book.availableImmediately(DomainSide.BUY, true, null),
                "40+60 -> after -10 on top level, total SELL depth = 30+60 = 90");

        // Full fill remaining 30 on the best ask
        book.setQtyLeaves(contra, 0L);
        // Pop top-of-book now that it’s fully consumed
        book.popBestContra(DomainSide.BUY);

        // Next best is 101.00 × 60
        Optional<RestingRef> mayBeNext = book.bestContra(DomainSide.BUY);
        assertTrue(mayBeNext.isPresent(), "next contra should be present for BUY taker");
        RestingRef next = mayBeNext.get();
        assertNotNull(next, "second level should now be best");
        assertEquals(60L, book.qtyLeavesOf(next));
        assertEquals(60L, book.availableImmediately(DomainSide.BUY, true, null),
                "only the 101.00 × 60 level remains");
    }

    @Test
    @DisplayName("SELL taker symmetry: bestContra is best bid; pop after full consumption")
    void sellTaker_contraFlow() {
        // Initial availability (SELL market consumes all BUY depth)
        assertEquals(120L, book.availableImmediately(DomainSide.SELL, true, null));

        Optional<RestingRef> maybeContra = book.bestContra(DomainSide.SELL); // best bid 99.50 × 50
        assertTrue(maybeContra.isPresent(), "best contra should be present for SELL taker");
        RestingRef contra = maybeContra.get();

        assertNotNull(contra);
        assertEquals(50L, book.qtyLeavesOf(contra));

        // Fully consume top bid
        book.setQtyLeaves(contra, 0L);
        book.popBestContra(DomainSide.SELL);

        // Next best bid is 99.00 × 70
        Optional<RestingRef> maybeNext = book.bestContra(DomainSide.SELL);
        assertTrue(maybeNext.isPresent(), "next contra should be present for SELL taker");
        RestingRef next = maybeNext.get();
        assertNotNull(next);
        assertEquals(70L, book.qtyLeavesOf(next));

        assertEquals(70L, book.availableImmediately(DomainSide.SELL, true, null),
                "remaining BUY depth is 70 at 99.00");
    }

    // --------- WIRE THESE TO YOUR REAL ADD/REST API ---------


    /**
     * Rest a SELL order at the given price/qty with monotonic time.
     */
    private void restSell(OrderBook book, long pxMicros, long qty) {
        // Minimal mocks; equals/hashCode by identity is good enough for map keys.
        var vo = mock(VenueOrder.class);
        var childId = mock(ChildId.class);
        book.addResting(vo, childId, DomainSide.SELL, pxMicros, qty, dualTimeSource.nowNanos());
    }

    /**
     * Rest a BUY order at the given price/qty with monotonic time.
     */
    private void restBuy(OrderBook book, long pxMicros, long qty) {
        var vo = mock(VenueOrder.class);
        var childId = mock(ChildId.class);
        book.addResting(vo, childId, DomainSide.BUY, pxMicros, qty, dualTimeSource.nowNanos());
    }

}
