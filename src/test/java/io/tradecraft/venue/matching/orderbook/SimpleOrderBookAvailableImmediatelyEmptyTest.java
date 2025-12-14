package io.tradecraft.venue.matching.orderbook;

import io.tradecraft.common.domain.market.DomainSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleOrderBookAvailableImmediatelyEmptyTest {

    @Test
    @DisplayName("availableImmediately = 0 for market and limit on an empty book (both sides)")
    void emptyBook_returnsZero() {
        OrderBook book = new SimpleOrderBook();

        // BUY taker
        assertEquals(0L, book.availableImmediately(DomainSide.BUY, true, null), "BUY market on empty");
        assertEquals(0L, book.availableImmediately(DomainSide.BUY, false, 100_00L), "BUY limit on empty");

        // SELL taker
        assertEquals(0L, book.availableImmediately(DomainSide.SELL, true, null), "SELL market on empty");
        assertEquals(0L, book.availableImmediately(DomainSide.SELL, false, 100_00L), "SELL limit on empty");
    }
}
