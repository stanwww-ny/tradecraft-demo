package io.tradecraft.venue.strategy;

import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.util.VenueCommandTestFactory;
import io.tradecraft.venue.api.VenueExecution;
import io.tradecraft.venue.cmd.CancelChildCmd;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.cmd.ReplaceChildCmd;
import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueFill;
import io.tradecraft.venue.matching.MatchingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for MatchingEngineStrategy
 */
public class MatchingEngineStrategyTest {

    private final DualTimeSource dualTimeSource = TestClocks.msTicker();
    private MatchingEngine engine;
    private MatchingEngineStrategy strategy;

    @BeforeEach
    void setup() {
        engine = mock(MatchingEngine.class);
        strategy = new MatchingEngineStrategy(engine);
    }

    private NewChildCmd newLimit(DomainSide side, long px, long qty) {
        return VenueCommandTestFactory.newChildCmd(side, qty, DomainOrdType.LIMIT, px, dualTimeSource);
    }

    @Test
    @DisplayName("Aggressive BUY LIMIT → strategy.decide delegates to engine.onNew and returns its VenueExecution")
    void aggressiveBuyDelegatesToEngine() {
        NewChildCmd c = newLimit(DomainSide.BUY, 101_00L, 100L);

        VenueAck ack = mock(VenueAck.class);
        VenueFill fill = mock(VenueFill.class);
        when(fill.lastPxMicros()).thenReturn(101_00L);
        when(fill.lastQty()).thenReturn(100L);

        VenueExecution expected = VenueExecution.of(List.of(ack), List.of(fill), null, null);
        when(engine.onNew(c)).thenReturn(expected);

        VenueExecution exec = strategy.decide(c);

        assertSame(expected, exec, "Should return exactly what MatchingEngine returns");
        verify(engine, times(1)).onNew(c);
        verify(engine, never()).onCancel(any());
        verify(engine, never()).onReplace(any());
    }

    @Test
    @DisplayName("Passive SELL LIMIT → strategy.decide delegates to engine.onNew (ack only path)")
    void passiveSellDelegatesToEngine() {
        NewChildCmd c = newLimit(DomainSide.SELL, 105_00L, 200L);

        VenueAck ack = mock(VenueAck.class);
        VenueExecution expected = VenueExecution.of(List.of(ack), List.of(), null, null);
        when(engine.onNew(c)).thenReturn(expected);

        VenueExecution exec = strategy.decide(c);

        assertSame(expected, exec);
        assertEquals(1, exec.acks().size());
        assertTrue(exec.fills().isEmpty());
        verify(engine).onNew(c);
        verify(engine, never()).onCancel(any());
        verify(engine, never()).onReplace(any());
    }

    @Test
    @DisplayName("Cancel → strategy.decide delegates to engine.onCancel")
    void cancelDelegatesToEngine() {
        CancelChildCmd x = mock(CancelChildCmd.class);

        VenueAck cancelAck = mock(VenueAck.class);
        VenueExecution expected = VenueExecution.of(List.of(cancelAck), List.of(), null, null);
        when(engine.onCancel(x)).thenReturn(expected);

        VenueExecution exec = strategy.decide(x);

        assertSame(expected, exec);
        verify(engine, times(1)).onCancel(x);
        verify(engine, never()).onNew(any());
        verify(engine, never()).onReplace(any());
    }

    @Test
    @DisplayName("Replace → strategy.decide delegates to engine.onReplace")
    void replaceDelegatesToEngine() {
        ReplaceChildCmd r = mock(ReplaceChildCmd.class);

        VenueAck replaceAck = mock(VenueAck.class);
        VenueExecution expected = VenueExecution.of(List.of(replaceAck), List.of(), null, null);
        when(engine.onReplace(r)).thenReturn(expected);

        VenueExecution exec = strategy.decide(r);

        assertSame(expected, exec);
        verify(engine, times(1)).onReplace(r);
        verify(engine, never()).onNew(any());
        verify(engine, never()).onCancel(any());
    }
}
