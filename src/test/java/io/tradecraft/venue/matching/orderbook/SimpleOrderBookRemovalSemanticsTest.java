package io.tradecraft.venue.matching.orderbook;

import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.testing.TestClocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SimpleOrderBookRemovalSemanticsTest {

    private final DualTimeSource dualTimeSource = TestClocks.msTicker();
    private OrderBook book;

    @BeforeEach
    void setUp() {
        book = new SimpleOrderBook();

        // SELL book (contra to BUY): best ask 100.50 × 40, then 101.00 × 60
        restSell(100_50L, 40L);
        restSell(101_00L, 60L);

        // BUY book (contra to SELL): best bid 99.50 × 50, then 99.00 × 70
        restBuy(99_50L, 50L);
        restBuy(99_00L, 70L);
    }

    @Test
    @DisplayName("Removing best SELL contra after full consumption advances best and updates totals")
    void removeAfterTrade_buyTaker() {
        // Initial: BUY market can take all SELL depth (100)
        assertEquals(100L, book.availableImmediately(DomainSide.BUY, true, null));

        // Best contra (SELL) is 100.50 × 40
        Optional<RestingRef> maybeTop = book.bestContra(DomainSide.BUY);
        assertTrue(maybeTop.isPresent());
        RestingRef top = maybeTop.get();
        assertEquals(40L, book.qtyLeavesOf(top));

        // Fill remaining qty to zero
        book.setQtyLeaves(top, 0L);

        // Remove the fully-consumed resting ref
        book.popBestContra(DomainSide.BUY);

        // Next best becomes 101.00 × 60; totals drop accordingly
        Optional<RestingRef> maybeNext = book.bestContra(DomainSide.BUY);
        assertTrue(maybeNext.isPresent());
        assertEquals(60L, book.qtyLeavesOf(maybeNext.get()));
        assertEquals(60L, book.availableImmediately(DomainSide.BUY, true, null));
    }

    @Test
    @DisplayName("Removing best BUY contra after full consumption advances best and updates totals")
    void removeAfterTrade_sellTaker() {
        // Initial: SELL market can take all BUY depth (120)
        assertEquals(120L, book.availableImmediately(DomainSide.SELL, true, null));

        // Best contra (BUY) is 99.50 × 50
        Optional<RestingRef> maybeTop = book.bestContra(DomainSide.SELL);
        assertTrue(maybeTop.isPresent());
        RestingRef top = maybeTop.get();
        assertEquals(50L, book.qtyLeavesOf(top));

        // Fill to zero and pop
        book.setQtyLeaves(top, 0L);
        book.popBestContra(DomainSide.SELL);

        // Next best is 99.00 × 70; totals drop accordingly
        Optional<RestingRef> maybeNext = book.bestContra(DomainSide.SELL);
        assertTrue(maybeNext.isPresent());
        assertEquals(70L, book.qtyLeavesOf(maybeNext.get()));
        assertEquals(70L, book.availableImmediately(DomainSide.SELL, true, null));
    }

    @Test
    @DisplayName("When last level is removed, bestContra becomes empty and availableImmediately=0")
    void removeLastLevel_yieldsEmptyBookForThatSide() {
        // Remove both SELL levels
        // 1) pop 100.50 × 40
        RestingRef a = book.bestContra(DomainSide.BUY).orElseThrow();
        book.setQtyLeaves(a, 0L);
        book.popBestContra(DomainSide.BUY);

        // 2) pop 101.00 × 60
        RestingRef b = book.bestContra(DomainSide.BUY).orElseThrow();
        book.setQtyLeaves(b, 0L);
        book.popBestContra(DomainSide.BUY);

        // Now there is no SELL contra left
        assertTrue(book.bestContra(DomainSide.BUY).isEmpty(), "no contra after removing last level");
        assertEquals(0L, book.availableImmediately(DomainSide.BUY, true, null));
    }

    // ---------- wiring helpers (addResting) ----------

    private void restSell(long pxMicros, long qty) {
        var vo = mock(io.tradecraft.venue.model.VenueOrder.class);
        var childId = mock(ChildId.class);
        ((SimpleOrderBook) book).addResting(vo, childId, DomainSide.SELL, pxMicros, qty, dualTimeSource.nowNanos());
    }

    private void restBuy(long pxMicros, long qty) {
        var vo = mock(io.tradecraft.venue.model.VenueOrder.class);
        var childId = mock(ChildId.class);
        ((SimpleOrderBook) book).addResting(vo, childId, DomainSide.BUY, pxMicros, qty, dualTimeSource.nowNanos());
    }

}

