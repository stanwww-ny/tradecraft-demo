package io.tradecraft.venue.matching.orderbook;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.venue.model.VenueOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Contract checked: - BUY limit L  => sum of SELL contra with price <= L - SELL limit L => sum of BUY  contra with
 * price >= L - Market       => sum of all contra liquidity (price ignored)
 * <p>
 * Book seeded with: SELL (asks): 100.50 × 40, 101.00 × 60  => total 100 BUY  (bids):  99.50 × 50,  99.00 × 70  => total
 * 120
 */
class SimpleOrderBookAvailableImmediatelyPopulatedTest {

    private final DualTimeSource dualTimeSource = TestClocks.msTicker();
    private OrderBook book;

    @BeforeEach
    void setUp() {
        book = new SimpleOrderBook();

        // SELL side (contra to BUY)
        restSell(100_50L, 40L);
        restSell(101_00L, 60L);

        // BUY side (contra to SELL)
        restBuy(99_50L, 50L);
        restBuy(99_00L, 70L);
    }

    // ---------------- BUY taker (consumes SELL contra) ----------------

    @Test
    @DisplayName("BUY market: consumes all SELL depth (100)")
    void buyMarket_allSellDepth() {
        assertEquals(100L, book.availableImmediately(DomainSide.BUY, true, null));
    }

    @Test
    @DisplayName("BUY limit 100.60: only 100.50 level (≤ L)")
    void buyLimit_oneLevel() {
        assertEquals(40L, book.availableImmediately(DomainSide.BUY, false, 100_60L));
    }

    @Test
    @DisplayName("BUY limit 101.00: includes 100.50 and 101.00 (≤ L)")
    void buyLimit_twoLevels() {
        assertEquals(100L, book.availableImmediately(DomainSide.BUY, false, 101_00L));
    }

    @Test
    @DisplayName("BUY limit 100.40: none (no ask ≤ L)")
    void buyLimit_none() {
        assertEquals(0L, book.availableImmediately(DomainSide.BUY, false, 100_40L));
    }

    // ---------------- SELL taker (consumes BUY contra) ----------------

    @Test
    @DisplayName("SELL market: consumes all BUY depth (120)")
    void sellMarket_allBuyDepth() {
        assertEquals(120L, book.availableImmediately(DomainSide.SELL, true, null));
    }

    @Test
    @DisplayName("SELL limit 99.50: only 99.50 level (≥ L)")
    void sellLimit_atBestBid_onlyTopLevel() {
        long qty = book.availableImmediately(DomainSide.SELL, false, 99_50L);
        assertEquals(50L, qty); // 99.00 is < 99.50, so excluded
    }

    @Test
    @DisplayName("SELL limit 99.40: includes 99.50 only (≥ L)")
    void sellLimit_aboveSecondLevel() {
        long qty = book.availableImmediately(DomainSide.SELL, false, 99_40L);
        assertEquals(50L, qty);
    }

    @Test
    @DisplayName("SELL limit 99.00: includes 99.50 and 99.00 (≥ L)")
    void sellLimit_atSecondLevel_includesBoth() {
        long qty = book.availableImmediately(DomainSide.SELL, false, 99_00L);
        assertEquals(120L, qty);
    }

    @Test
    @DisplayName("SELL limit 98.00: includes both levels (≥ L)")
    void sellLimit_lowLimit_includesBoth() {
        long qty = book.availableImmediately(DomainSide.SELL, false, 98_00L);
        assertEquals(120L, qty);
    }

    @Test
    @DisplayName("SELL limit 99.60: none (no bid ≥ L)")
    void sellLimit_tooHigh_none() {
        long qty = book.availableImmediately(DomainSide.SELL, false, 99_60L);
        assertEquals(0L, qty);
    }

    // -------------------------- wiring --------------------------

    private void restSell(long pxMicros, long qty) {
        var vo = mock(VenueOrder.class);
        var childId = mock(ChildId.class);
        ((SimpleOrderBook) book).addResting(vo, childId, DomainSide.SELL, pxMicros, qty, dualTimeSource.nowNanos());
    }

    private void restBuy(long pxMicros, long qty) {
        var vo = mock(VenueOrder.class);
        var childId = mock(ChildId.class);
        ((SimpleOrderBook) book).addResting(vo, childId, DomainSide.BUY, pxMicros, qty, dualTimeSource.nowNanos());
    }
}
