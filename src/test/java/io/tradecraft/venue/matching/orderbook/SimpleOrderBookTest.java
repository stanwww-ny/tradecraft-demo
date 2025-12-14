package io.tradecraft.venue.matching.orderbook;

import io.tradecraft.common.domain.market.DomainSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimpleOrderBookEmptyStateTest {

    @Test
    @DisplayName("Fresh SimpleOrderBook has no contra levels and zero available qty")
    void emptyBook_hasNoLiquidity() {
        OrderBook book = new SimpleOrderBook();

        // SELL taker looks at BUY contra; BUY taker looks at SELL contra
        Iterator<RestingRef> sellTakerContra = book.iterateContraBestFirst(DomainSide.SELL);
        Iterator<RestingRef> buyTakerContra = book.iterateContraBestFirst(DomainSide.BUY);

        assertNotNull(sellTakerContra, "iterator must not be null");
        assertNotNull(buyTakerContra, "iterator must not be null");
        assertFalse(sellTakerContra.hasNext(), "empty book should yield empty contra iterator for SELL taker");
        assertFalse(buyTakerContra.hasNext(), "empty book should yield empty contra iterator for BUY taker");

        // Market taker: zero available
        assertEquals(0L, book.availableImmediately(DomainSide.BUY, true, null));
        assertEquals(0L, book.availableImmediately(DomainSide.SELL, true, null));

        // Limit taker: zero available (any limit)
        assertEquals(0L, book.availableImmediately(DomainSide.BUY, false, 100_00L));
        assertEquals(0L, book.availableImmediately(DomainSide.SELL, false, 100_00L));
    }
}
