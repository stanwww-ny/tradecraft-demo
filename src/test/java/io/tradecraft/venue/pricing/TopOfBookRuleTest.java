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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TopOfBookRuleTest {

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
    void marketBuy_executesAtAsk_whenAskPresent() {
        // NBBO: bid=100_000_000 (100.00), ask=100_100_000 (100.10)
        nbboUpdater.onTopOfBookUpdate(100_000_000L, 100_100_000L, dualTimeSource.nowNanos());
        TopOfBookRule rule = new TopOfBookRule(nbboProvider);
        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.BUY).ordType(DomainOrdType.MARKET).build();
        VenueOrder vo = null;

        assertTrue(rule.appliesTo(cmd, vo));
        assertEquals(100_100_000L, rule.priceMicros(cmd, vo));
    }

    @Test
    void marketSell_executesAtBid_whenBidPresent() {
        nbboUpdater.onTopOfBookUpdate(100_000_000L, 100_100_000L, dualTimeSource.nowNanos());
        TopOfBookRule rule = new TopOfBookRule(nbboProvider);
        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.SELL).ordType(DomainOrdType.MARKET).build();
        VenueOrder vo = null;

        assertTrue(rule.appliesTo(cmd, vo));
        assertEquals(100_000_000L, rule.priceMicros(cmd, vo));
    }

    @Test
    void marketBuy_doesNotApply_whenAskMissing() {
        nbboUpdater.onTopOfBookUpdate(100_000_000L, null, dualTimeSource.nowNanos());
        TopOfBookRule rule = new TopOfBookRule(nbboProvider);
        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.BUY).ordType(DomainOrdType.MARKET).build();
        assertFalse(rule.appliesTo(cmd, null));
    }

    @Test
    void marketSell_doesNotApply_whenBidMissing() {
        nbboUpdater.onTopOfBookUpdate(null, 100_100_000L, dualTimeSource.nowNanos());
        TopOfBookRule rule = new TopOfBookRule(nbboProvider);
        NewChildCmd cmd = NewChildCmd.builder().side(DomainSide.SELL).ordType(DomainOrdType.MARKET).build();
        assertFalse(rule.appliesTo(cmd, null));
    }

    @Test
    void limitOrder_neverApplies() {
        nbboUpdater.onTopOfBookUpdate(100_000_000L, 100_100_000L, dualTimeSource.nowNanos());
        TopOfBookRule rule = new TopOfBookRule(nbboProvider);
        NewChildCmd cmdBuy = NewChildCmd.builder().side(DomainSide.BUY).ordType(DomainOrdType.LIMIT).build();
        NewChildCmd cmdSell = NewChildCmd.builder().side(DomainSide.SELL).ordType(DomainOrdType.LIMIT).build();

        assertFalse(rule.appliesTo(cmdBuy, null));
        assertFalse(rule.appliesTo(cmdSell, null));
    }
}

