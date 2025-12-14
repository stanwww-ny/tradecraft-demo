// src/test/java/io/tradecraft/venueId/strategy/ImmediateFillStrategyTest.java
package io.tradecraft.venue.strategy;

import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.venue.api.VenueExecution;
import io.tradecraft.venue.api.VenueSupport;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueFill;
import io.tradecraft.venue.matching.orderbook.FillSource;
import io.tradecraft.venue.model.VenueOrder;
import io.tradecraft.venue.nbbo.NbboCache;
import io.tradecraft.venue.nbbo.NbboProvider;
import io.tradecraft.venue.nbbo.NbboUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers: market buy/sell using NBBO, crossable limit, and non-crossable noop.
 */
@ExtendWith(MockitoExtension.class)
class ImmediateFillStrategyTest {

    @Mock
    private VenueSupport venueSupport;
    @Mock
    private NewChildCmd cmd;
    @Mock
    private VenueOrder vo;
    @Mock
    private VenueAck ack;
    @Mock
    private VenueFill fill;
    @Mock
    private DualTimeSource ts;

    private NbboCache nbbo;            // implements NbboProvider + NbboUpdater
    private NbboProvider nbboProvider; // read handle for the strategy
    private NbboUpdater nbboUpdater;   // write handle for the test
    private ImmediateFillStrategy strategy;

    @BeforeEach
    void setUp() {
        nbbo = new NbboCache();
        nbboProvider = nbbo;
        nbboUpdater = nbbo;

        when(venueSupport.nbboProvider()).thenReturn(nbboProvider);

        strategy = new ImmediateFillStrategy(venueSupport);
    }

    // --- Helpers -------------------------------------------------------------

    private void stubMarket(NewChildCmd c, DomainSide side) {
        when(c.ordType()).thenReturn(DomainOrdType.MARKET);
        when(c.side()).thenReturn(side);
    }

    private void stubLimit(NewChildCmd c, DomainSide side, long limitPxMicros) {
        when(c.ordType()).thenReturn(DomainOrdType.LIMIT);
        when(c.side()).thenReturn(side);
        // adapt if your API uses priceMicros() vs limitPxMicros()
        try {
            when(c.priceMicros()).thenReturn(limitPxMicros);
        } catch (Throwable ignore) {
            // fall back to limitPxMicros if that's the name in your codebase
            try {
                when(c.priceMicros()).thenReturn(limitPxMicros);
            } catch (Throwable ignored) {
            }
        }
    }

    private void captureFillAndReturnMock(ArgumentCaptor<Long> pxCap) {
        // Capture the price the strategy computed and return a fill mock that reports it
        when(venueSupport.applyFill(eq(vo), eq(100L), pxCap.capture(), eq(true), eq(FillSource.MATCHING_ENGINE)))
                .thenAnswer(inv -> {
                    long px = inv.getArgument(2, Long.class);
                    when(fill.lastPxMicros()).thenReturn(px);
                    when(fill.lastQty()).thenReturn(100L);
                    return fill;
                });
    }

    // --- Tests ---------------------------------------------------------------

    @Test
    void marketBuy_liftsAsk() {
        // Correct NBBO micros
        nbboUpdater.onTopOfBookUpdate(200_000_000L, 201_000_000L, 1L);

        when(cmd.ordType()).thenReturn(DomainOrdType.MARKET);
        when(cmd.side()).thenReturn(DomainSide.BUY);

        // Only stub what this test uses:
        when(venueSupport.create(cmd)).thenReturn(vo);
        when(vo.qty()).thenReturn(100L);
        when(venueSupport.ack(cmd, vo)).thenReturn(ack);

        ArgumentCaptor<Long> pxCap = ArgumentCaptor.forClass(Long.class);
        // Build a VenueFill using the captured price
        when(venueSupport.applyFill(eq(vo), eq(100L), pxCap.capture(), eq(true), eq(FillSource.MATCHING_ENGINE)))
                .thenAnswer(inv -> {
                    long p = inv.getArgument(2, Long.class);
                    VenueFill f = mock(VenueFill.class);
                    when(f.lastPxMicros()).thenReturn(p);
                    when(f.lastQty()).thenReturn(100L);
                    return f;
                });

        VenueExecution ex = strategy.decide(cmd);

        assertEquals(1, ex.acks().size());
        assertEquals(1, ex.fills().size());
        assertEquals(201_000_000L, ex.fills().get(0).lastPxMicros());
        assertEquals(100L, ex.fills().get(0).lastQty());
        assertEquals(201_000_000L, pxCap.getValue());
    }


    @Test
    void MarketSell_hitsBid() {
        nbboUpdater.onTopOfBookUpdate(200_500_000L, 200_800_000L, 2L);

        when(cmd.ordType()).thenReturn(DomainOrdType.MARKET);
        when(cmd.side()).thenReturn(DomainSide.SELL);

        when(venueSupport.create(cmd)).thenReturn(vo);
        when(vo.qty()).thenReturn(100L);
        when(venueSupport.ack(cmd, vo)).thenReturn(ack);

        ArgumentCaptor<Long> pxCap = ArgumentCaptor.forClass(Long.class);
        when(venueSupport.applyFill(eq(vo), eq(100L), pxCap.capture(),
                eq(true), eq(FillSource.MATCHING_ENGINE)))
                .thenAnswer(inv -> {
                    long p = inv.getArgument(2, Long.class);
                    VenueFill f = mock(VenueFill.class);
                    when(f.lastPxMicros()).thenReturn(p);
                    when(f.lastQty()).thenReturn(100L);
                    return f;
                });

        var ex = strategy.decide(cmd);

        assertEquals(200_500_000L, ex.fills().get(0).lastPxMicros());
        assertEquals(100L, ex.fills().get(0).lastQty());
        assertEquals(200_500_000L, pxCap.getValue());
    }

    @Test
    void crossableLimitBuy_usesAsk() {
        nbboUpdater.onTopOfBookUpdate(200_000_000L, 201_000_000L, 3L);

        when(cmd.ordType()).thenReturn(DomainOrdType.LIMIT);
        when(cmd.side()).thenReturn(DomainSide.BUY);
        // whichever your API exposes:
        try {
            when(cmd.priceMicros()).thenReturn(201_000_000L);
        } catch (Throwable ignore) {
        }

        when(venueSupport.create(cmd)).thenReturn(vo);
        when(vo.qty()).thenReturn(100L);
        when(venueSupport.ack(cmd, vo)).thenReturn(ack);

        ArgumentCaptor<Long> pxCap = ArgumentCaptor.forClass(Long.class);
        when(venueSupport.applyFill(eq(vo), eq(100L), pxCap.capture(),
                eq(true), eq(FillSource.MATCHING_ENGINE)))
                .thenAnswer(inv -> {
                    long p = inv.getArgument(2, Long.class);
                    VenueFill f = mock(VenueFill.class);
                    when(f.lastPxMicros()).thenReturn(p);
                    when(f.lastQty()).thenReturn(100L);
                    return f;
                });

        var ex = strategy.decide(cmd);

        assertEquals(201_000_000L, ex.fills().get(0).lastPxMicros());
        assertEquals(100L, ex.fills().get(0).lastQty());
        assertEquals(201_000_000L, pxCap.getValue());
    }

    @Test
    void nonCrossableLimit_noop_goesToMatchingEngineLater() {
        // ask = 201.00, limit = 200.00 (not crossable)
        nbboUpdater.onTopOfBookUpdate(200_00_00L, 201_00_00L, 4L);
        stubLimit(cmd, DomainSide.BUY, 200_00_00L);

        // Strategy should not call applyFill at all
        VenueExecution ex = strategy.decide(cmd);

        // If your VenueExecution has isNoop(), use that. Otherwise, check empty acks/fills.
        boolean hasIsNoop;
        try {
            var m = VenueExecution.class.getMethod("isNoop");
            hasIsNoop = true;
            assertTrue((boolean) m.invoke(ex));
        } catch (Exception reflective) {
            hasIsNoop = false;
        }

        if (!hasIsNoop) {
            assertTrue(ex.acks() == null || ex.acks().isEmpty());
            assertTrue(ex.fills() == null || ex.fills().isEmpty());
        }

        verify(venueSupport, never()).applyFill(any(), anyLong(), anyLong(), anyBoolean(), any());
    }

    @Test
    void missingNbbo_noop_or_reject_by_policy() {
        // Do NOT publish NBBO → provider has empty snapshot
        stubMarket(cmd, DomainSide.BUY);

        VenueExecution ex = strategy.decide(cmd);

        // Depending on your policy this could be noop (preferred) or a reject.
        // We just assert it isn't a fill.
        assertTrue(ex.fills() == null || ex.fills().isEmpty());
    }
}
