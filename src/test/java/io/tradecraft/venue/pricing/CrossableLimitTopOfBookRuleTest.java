package io.tradecraft.venue.pricing;

import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.model.VenueOrder;
import io.tradecraft.venue.nbbo.NbboCache;
import io.tradecraft.venue.nbbo.NbboProvider;
import io.tradecraft.venue.nbbo.NbboUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CrossableLimitTopOfBookRuleTest {

    private final DualTimeSource dualTimeSource = TestClocks.msTicker();
    private final NbboCache nbbo = new NbboCache();
    NbboProvider nbboProvider;
    NbboUpdater nbboUpdater;

    @BeforeEach
    void setup() {
        nbboProvider = nbbo;
        nbboUpdater = nbbo;
    }

    @Test
    void buyLimit_equalToAsk_isCrossable_executesAtAsk() {
        nbboUpdater.onTopOfBookUpdate(199_400_000L, 199_500_000L, dualTimeSource.nowNanos());


        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.BUY)
                .ordType(DomainOrdType.LIMIT).priceMicros(199_500_000L).build();

        VenueOrder vo = mock(VenueOrder.class, Answers.RETURNS_DEEP_STUBS);

        CrossableLimitTopOfBookRule rule = new CrossableLimitTopOfBookRule(nbbo);

        assertTrue(rule.appliesTo(cmd, vo));
        assertEquals(199_500_000L, rule.priceMicros(cmd, vo));  // ask
    }

    @Test
    void buyLimit_aboveAsk_isCrossable_executesAtAsk() {
        nbboUpdater.onTopOfBookUpdate(199_400_000L, 199_500_000L, dualTimeSource.nowNanos());

        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.BUY)
                .ordType(DomainOrdType.LIMIT).priceMicros(199_600_000L).build();

        CrossableLimitTopOfBookRule rule = new CrossableLimitTopOfBookRule(nbboProvider);

        assertTrue(rule.appliesTo(cmd, null));
        assertEquals(199_500_000L, rule.priceMicros(cmd, null)); // ask
    }

    @Test
    void buyLimit_belowAsk_notCrossable_ruleDoesNotApply() {
        nbboUpdater.onTopOfBookUpdate(199_400_000L, 199_500_000L, dualTimeSource.nowNanos());

        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.BUY)
                .ordType(DomainOrdType.LIMIT).priceMicros(199_490_000L).build();

        CrossableLimitTopOfBookRule rule = new CrossableLimitTopOfBookRule(nbboProvider);

        assertFalse(rule.appliesTo(cmd, null));
    }

    @Test
    void sellLimit_equalToBid_isCrossable_executesAtBid() {
        nbboUpdater.onTopOfBookUpdate(199_500_000L, 199_600_000L, dualTimeSource.nowNanos());
        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.SELL)
                .ordType(DomainOrdType.LIMIT).priceMicros(199_500_000L).build();

        CrossableLimitTopOfBookRule rule = new CrossableLimitTopOfBookRule(nbboProvider);

        assertTrue(rule.appliesTo(cmd, null));
        assertEquals(199_500_000L, rule.priceMicros(cmd, null)); // bid
    }

    @Test
    void sellLimit_belowBid_isCrossable_executesAtBid() {
        nbboUpdater.onTopOfBookUpdate(199_500_000L, 199_600_000L, dualTimeSource.nowNanos());
        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.SELL)
                .ordType(DomainOrdType.LIMIT).priceMicros(199_400_000L).build();

        CrossableLimitTopOfBookRule rule = new CrossableLimitTopOfBookRule(nbboProvider);

        assertTrue(rule.appliesTo(cmd, null));
        assertEquals(199_500_000L, rule.priceMicros(cmd, null)); // bid
    }

    @Test
    void sellLimit_aboveBid_notCrossable_ruleDoesNotApply() {
        nbboUpdater.onTopOfBookUpdate(199_500_000L, 199_600_000L, dualTimeSource.nowNanos());
        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.SELL)
                .ordType(DomainOrdType.LIMIT).priceMicros(199_510_000L).build();

        CrossableLimitTopOfBookRule rule = new CrossableLimitTopOfBookRule(nbboProvider);

        assertFalse(rule.appliesTo(cmd, null));
    }

    @Test
    void missingLimitPrice_ruleDoesNotApply() {
        nbboUpdater.onTopOfBookUpdate(199_500_000L, 199_600_000L, dualTimeSource.nowNanos());
        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.SELL)
                .ordType(DomainOrdType.LIMIT).priceMicros(null).build();

        CrossableLimitTopOfBookRule rule = new CrossableLimitTopOfBookRule(nbboProvider);
        assertFalse(rule.appliesTo(cmd, null));
    }

    @Test
    void missingAsk_forBuy_ruleDoesNotApply() {
        nbboUpdater.onTopOfBookUpdate(100_000_000L, null, dualTimeSource.nowNanos());
        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.BUY)
                .ordType(DomainOrdType.LIMIT).priceMicros(101_000_000L).build();

        CrossableLimitTopOfBookRule rule = new CrossableLimitTopOfBookRule(nbboProvider);
        assertFalse(rule.appliesTo(cmd, null));
    }

    @Test
    void missingBid_forSell_ruleDoesNotApply() {
        nbboUpdater.onTopOfBookUpdate(null, 101_000_000L, dualTimeSource.nowNanos());

        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.SELL)
                .ordType(DomainOrdType.LIMIT).priceMicros(99_000_000L).build();

        CrossableLimitTopOfBookRule rule = new CrossableLimitTopOfBookRule(nbboProvider);
        assertFalse(rule.appliesTo(cmd, null));
    }

    @Test
    void marketOrders_neverApply() {
        nbboUpdater.onTopOfBookUpdate(null, null, dualTimeSource.nowNanos());

        NewChildCmd buyMkt = NewChildCmd.builder().ordType(DomainOrdType.LIMIT).build();
        NewChildCmd sellMkt = NewChildCmd.builder().ordType(DomainOrdType.MARKET).build();

        CrossableLimitTopOfBookRule rule = new CrossableLimitTopOfBookRule(nbboProvider);

        assertFalse(rule.appliesTo(buyMkt, null));
        assertFalse(rule.appliesTo(sellMkt, null));
    }
}
