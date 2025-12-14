package io.tradecraft.venue.matching.orderbook;

import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.testing.TestClocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SimpleOrderBookPriceTimePrioritySellTest {

    private final DualTimeSource dualTimeSource = TestClocks.msTicker();
    private OrderBook book;

    @BeforeEach
    void setUp() {
        book = new SimpleOrderBook();
        // Price priority: 100.50 better than 101.00
        // Time priority at 100.50: sA earlier than sB
        restSell(101_00L, 30L);               // worse price
        restSell(100_50L, 10L);               // best price, earlier time
        restSell(100_50L, 20L);               // best price, later time
    }

    @Test
    @DisplayName("bestContra for BUY sees lowest ask, earliest time within level")
    void best_is_lowest_price_then_earliest_time() {
        // First best: 100.50 × 10 (earliest at that level)
        RestingRef r1 = book.bestContra(DomainSide.BUY).orElseThrow();
        assertEquals(10L, book.qtyLeavesOf(r1));

        // Consume r1 and pop -> next is the later 100.50 × 20
        book.setQtyLeaves(r1, 0L);
        book.popBestContra(DomainSide.BUY);

        RestingRef r2 = book.bestContra(DomainSide.BUY).orElseThrow();
        assertEquals(20L, book.qtyLeavesOf(r2));

        // Consume r2 and pop -> then 101.00 × 30
        book.setQtyLeaves(r2, 0L);
        book.popBestContra(DomainSide.BUY);

        RestingRef r3 = book.bestContra(DomainSide.BUY).orElseThrow();
        assertEquals(30L, book.qtyLeavesOf(r3));
    }

    // ---- helpers ----
    private void restSell(long pxMicros, long qty) {
        var vo = mock(io.tradecraft.venue.model.VenueOrder.class);
        var childId = mock(ChildId.class);
        ((SimpleOrderBook) book).addResting(vo, childId, DomainSide.SELL, pxMicros, qty, dualTimeSource.nowNanos());
    }

}
