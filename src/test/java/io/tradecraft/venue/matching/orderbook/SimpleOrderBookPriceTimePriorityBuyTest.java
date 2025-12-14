package io.tradecraft.venue.matching.orderbook;

import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.testing.TestClocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class SimpleOrderBookPriceTimePriorityBuyTest {

    private final DualTimeSource DualTimeSource = TestClocks.msTicker();
    private OrderBook book;

    @BeforeEach
    void setUp() {
        book = new SimpleOrderBook();
        // Price priority: 99.50 better than 99.00
        // Time priority at 99.50: bA earlier than bB
        restBuy(99_00L, 25L);                // worse price
        restBuy(99_50L, 15L);                // best price, earlier time
        restBuy(99_50L, 35L);                // best price, later time
    }

    @Test
    void best_is_highest_price_then_earliest_time() {
        var r1 = book.bestContra(DomainSide.SELL).orElseThrow();
        assertEquals(15L, book.qtyLeavesOf(r1));

        book.setQtyLeaves(r1, 0L);
        book.popBestContra(DomainSide.SELL);

        var r2 = book.bestContra(DomainSide.SELL).orElseThrow();
        assertEquals(35L, book.qtyLeavesOf(r2));

        book.setQtyLeaves(r2, 0L);
        book.popBestContra(DomainSide.SELL);

        var r3 = book.bestContra(DomainSide.SELL).orElseThrow();
        assertEquals(25L, book.qtyLeavesOf(r3));
    }

    private void restBuy(long pxMicros, long qty) {
        var vo = mock(io.tradecraft.venue.model.VenueOrder.class);
        var childId = mock(ChildId.class);
        ((SimpleOrderBook) book).addResting(vo, childId, DomainSide.BUY, pxMicros, qty, DualTimeSource.nowNanos());
    }
}
