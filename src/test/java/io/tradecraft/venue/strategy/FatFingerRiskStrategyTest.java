// src/test/java/<<your package>>/FatFingerRiskStrategyTest.java
package io.tradecraft.venue.strategy;

import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.util.VenueCommandTestFactory;
import io.tradecraft.venue.api.DefaultVenueSupport;
import io.tradecraft.venue.api.VenueExecution;
import io.tradecraft.venue.cmd.CancelChildCmd;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.cmd.ReplaceChildCmd;
import io.tradecraft.venue.nbbo.NbboProvider;
import io.tradecraft.venue.nbbo.NbboSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Assumptions about FatFingerRiskStrategy (adapt as needed):
 * <p>
 * - Constructor: new FatFingerRiskStrategy(NbboProvider nbbo, double upPct, double downPct, MatchingEngineStrategy
 * engine) - Behavior: * BUY LIMIT is rejected if limitPx > askPx * (1 + upPct) * SELL LIMIT is rejected if limitPx <
 * bidPx * (1 - downPct) * If NBBO has missing side (ask for BUY, bid for SELL) => reject * MARKET orders are allowed
 * (or skipped by this strategy) * On pass, delegate to engine.onNew(c) and return that VenueExecution * For
 * Cancel/Replace, delegate engine.onCancel/engine.onReplace
 */
public class FatFingerRiskStrategyTest {

    private final DualTimeSource dualTimeSource = TestClocks.msTicker();
    private DefaultVenueSupport support;
    private NbboProvider nbboProvider;
    private FatFingerRiskStrategy strategy; // SUT

    @BeforeEach
    void setUp() {
        support = mock(DefaultVenueSupport.class);
        nbboProvider = mock(NbboProvider.class);
        when(support.nbboProvider()).thenReturn(nbboProvider);
        when(support.dualTimeSource()).thenReturn(dualTimeSource);

        // 5% up/down bands for test; adjust to match your ctor/config.
        strategy = new FatFingerRiskStrategy(support, 0.05, 0.05);
    }

    // --- Helpers to build common mocks ---

    private NewChildCmd newLimitBuy(long pxMicros) {
        return VenueCommandTestFactory.newChildCmd(DomainSide.BUY, DomainOrdType.LIMIT, pxMicros, dualTimeSource);
    }

    private NewChildCmd newLimitSell(long pxMicros) {
        return VenueCommandTestFactory.newChildCmd(DomainSide.SELL, DomainOrdType.LIMIT, pxMicros, dualTimeSource);
    }

    private NewChildCmd newMarketBuy() {
        return VenueCommandTestFactory.newChildCmd(DomainSide.BUY, DomainOrdType.MARKET, 0, dualTimeSource);
    }

    private void setNbboSnapshot(Long bidPx, Long askPx) {
        NbboSnapshot nb = new NbboSnapshot(bidPx, askPx, support.dualTimeSource().nowNanos());
        when(support.nbboProvider().snapshot()).thenReturn(nb);
    }

    // --- Tests ---

    @Test
    @DisplayName("BUY LIMIT above allowed band → reject")
    void rejectBuyAboveBand() {
        // NBBO: bid=100.00, ask=101.00 (in micros)
        setNbboSnapshot(100_00L, 101_00L);

        // up band 5% over ask ⇒ threshold = 101 * 1.05 = 106.05
        NewChildCmd c = newLimitBuy(107_00L);
        VenueExecution exec = strategy.decide(c);

        // Assert
        assertTrue(exec.rejectOptional().isPresent(), "Expected reject execution");
    }

    @Test
    @DisplayName("SELL LIMIT below allowed band → reject")
    void rejectSellBelowBand() {
        // NBBO: bid=100.00, ask=101.00
        setNbboSnapshot(100_00L, 101_00L);

        // down band 5% under bid ⇒ threshold = 100 * 0.95 = 95.00
        NewChildCmd c = newLimitSell(94_00L);

        VenueExecution exec = strategy.decide(c);

        // Assert
        assertTrue(exec.rejectOptional().isPresent(), "Expected reject execution");
    }

    @Test
    @DisplayName("BUY LIMIT within band → delegates to engine.onNew")
    void passBuyWithinBand() {
        setNbboSnapshot(100_00L, 101_00L);
        NewChildCmd c = newLimitBuy(105_00L); // within 5% over ask = 106.05

        VenueExecution expected = VenueExecution.noop(); // or mock a real execution
        VenueExecution actual = strategy.decide(c);

        assertSame(expected, actual);
    }

    @Test
    @DisplayName("SELL LIMIT within band → delegates to engine.onNew")
    void passSellWithinBand() {
        setNbboSnapshot(100_00L, 101_00L);
        NewChildCmd c = newLimitSell(96_00L); // >= 95.00 threshold

        VenueExecution expected = VenueExecution.noop();
        VenueExecution actual = strategy.decide(c);
        assertSame(expected, actual);
    }

    @Test
    @DisplayName("Missing ask for BUY or missing bid for SELL → reject")
    void rejectWhenNbboSideMissing() {
        // BUY without ask
        setNbboSnapshot(100_00L, null);
        NewChildCmd c = newLimitBuy(105_00L);
        VenueExecution exec = strategy.decide(c);
        // Assert
        VenueExecution expected1 = VenueExecution.noop(); // or mock a real execution
        VenueExecution actual1 = strategy.decide(c);

        assertSame(expected1, actual1);

        // SELL without bid
        setNbboSnapshot(null, 101_00L);
        VenueExecution expected2 = VenueExecution.noop(); // or mock a real execution
        VenueExecution actual2 = strategy.decide(c);
        assertSame(expected2, actual2);
    }

    @Test
    @DisplayName("MARKET orders are not fat-fingered → delegated (or skipped)")
    void marketOrderPassesThrough() {
        // If your strategy ignores MARKET, it should just delegate (or return noop).
        setNbboSnapshot(100_00L, 101_00L);
        NewChildCmd c = newMarketBuy();

        VenueExecution expected = VenueExecution.noop();
        VenueExecution actual = strategy.decide(c);
        assertSame(expected, actual);
    }

    // Optional: prove price-band math right at the edge
    @Test
    @DisplayName("Edge: BUY at exactly ask * (1+upPct) should PASS")
    void buyAtEdgePasses() {
        setNbboSnapshot(100_00L, 101_00L);
        NewChildCmd c = newLimitBuy(106_05L); // within 5% over ask = 106.05

        VenueExecution expected = VenueExecution.noop(); // or mock a real execution
        VenueExecution actual = strategy.decide(c);

        assertSame(expected, actual);
    }

    @Test
    @DisplayName("Edge: SELL at exactly bid * (1-downPct) should PASS")
    void sellAtEdgePasses() {
        setNbboSnapshot(100_00L, 101_00L);
        // edge = 100 * 0.95 = 95.00
        NewChildCmd c = newLimitSell(95_00L);

        VenueExecution expected = VenueExecution.noop(); // or mock a real execution
        VenueExecution actual = strategy.decide(c);

        assertSame(expected, actual);
    }

    @Nested
    @DisplayName("Cancel/Replace pass-through")
    class CancelReplace {

        @Test
        void cancelDelegates() {
            CancelChildCmd x = mock(CancelChildCmd.class);
            VenueExecution expected = VenueExecution.noop();
            VenueExecution actual = strategy.decide(x);
            assertSame(expected, actual);
        }

        @Test
        void replaceDelegates() {
            ReplaceChildCmd r = mock(ReplaceChildCmd.class);
            VenueExecution expected = VenueExecution.noop();
            VenueExecution actual = strategy.decide(r);
            assertSame(expected, actual);
        }
    }
}
