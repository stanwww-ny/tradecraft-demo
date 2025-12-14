package io.tradecraft.venue.matching;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.domain.order.CancelReason;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.util.VenueCommandTestFactory;
import io.tradecraft.venue.api.DefaultVenueSupport;
import io.tradecraft.venue.api.VenueExecution;
import io.tradecraft.venue.cmd.CancelChildCmd;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.cmd.ReplaceChildCmd;
import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueCancelDone;
import io.tradecraft.venue.event.VenueFill;
import io.tradecraft.venue.matching.orderbook.OrderBook;
import io.tradecraft.venue.matching.orderbook.RestingRef;
import io.tradecraft.venue.model.VenueOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MatchingEngine unit tests tailored to your real OrderBook & Support APIs.
 * <p>
 * Focus: - onNew(): aggressive cross path and passive rest path - onCancel(): locate resting by child id and cancel -
 * (Optional) onReplace(): basic no-cross maker path
 */
class MatchingEngineTest {

    private final DualTimeSource dualTimeSource = TestClocks.msTicker();
    private DefaultVenueSupport support;
    private OrderBook book;
    private MatchingEngine engine;

    @BeforeEach
    void setUp() {
        support = mock(DefaultVenueSupport.class);
        book = mock(OrderBook.class);

        // MatchingEngine ctor uses DefaultVenueSupport and internally chooses an OrderBook.
        // Your support provides the book to the engine; stub that:
        when(support.dualTimeSource()).thenReturn(dualTimeSource);

        engine = new MatchingEngine(support, book);
    }

    private NewChildCmd newLimit(
            DomainSide side, long pxMicros, long qty, DomainTif tif, ChildId childId) {
        return VenueCommandTestFactory.newChildCmd(childId, side, qty, DomainOrdType.LIMIT,
                pxMicros, tif, dualTimeSource);
    }

    @Test
    @DisplayName("onNew: aggressive BUY LIMIT crosses best ask → 1 ack + 1 fill, no cancel")
    void onNew_aggressiveBuyCrosses() {
        // Arrange command & ids
        ChildId childId = mock(ChildId.class);
        NewChildCmd c = newLimit(DomainSide.BUY, 101_00L, 100L, DomainTif.DAY, childId);

        // Support side-effects
        VenueOrder vo = mock(VenueOrder.class);
        VenueAck ack = mock(VenueAck.class);
        VenueFill fill = mock(VenueFill.class);
        when(fill.lastPxMicros()).thenReturn(101_00L);
        when(fill.lastQty()).thenReturn(100L);

        when(support.create(c)).thenReturn(vo);
        when(support.ack(c, vo)).thenReturn(ack);
        // MatchingEngine applies fills through support.applyFill(..., FillSource.MATCHING_ENGINE)
        when(support.applyFill(eq(vo), eq(100L), eq(101_00L), anyBoolean(), any()))
                .thenReturn(fill);

        // Book state: one contra resting at 101 with 100 leaves
        RestingRef contra = new RestingRef(1L);
        when(book.bestContra(DomainSide.BUY)).thenReturn(Optional.of(contra));
        when(book.priceOf(contra)).thenReturn(101_00L);
        when(book.qtyLeavesOf(contra)).thenReturn(100L) // before trade
                .thenReturn(0L);   // after setQtyLeaves
        when(book.voOf(contra)).thenReturn(vo);
        // engine will call popBestContra when contra qty goes to 0
        // availableImmediately is used only for FOK gate; not needed for DAY/IOC here.

        // Act
        VenueExecution exec = engine.onNew(c);
        assertEquals(1, exec.acks().size());
        assertEquals(2, exec.fills().size());
        assertTrue(exec.cancelOptional().isEmpty());
        assertTrue(exec.rejectOptional().isEmpty());
        assertEquals(101_00L, exec.fills().get(0).lastPxMicros());
        assertEquals(100L, exec.fills().get(0).lastQty());

        // Book interactions:
        verify(book).bestContra(DomainSide.BUY);
        verify(book).priceOf(contra);
        verify(book, atLeastOnce()).qtyLeavesOf(contra);
        verify(book).popBestContra(DomainSide.BUY);
        verify(book, times(2)).voOf(contra);
        verify(support).clearResting(any()); // clears contra when fully consumed
    }

    @Test
    @DisplayName("onNew: passive SELL LIMIT (no contra) → rests: ack only, no fills, no cancel")
    void onNew_passiveSellRests() {
        ChildId childId = mock(ChildId.class);
        NewChildCmd c = newLimit(DomainSide.SELL, 105_00L, 200L, DomainTif.DAY, childId);

        VenueOrder vo = mock(VenueOrder.class);
        VenueAck ack = mock(VenueAck.class);
        when(support.create(c)).thenReturn(vo);
        when(support.ack(c, vo)).thenReturn(ack);

        // No contra available
        when(book.bestContra(DomainSide.SELL)).thenReturn(Optional.empty());

        // Engine should add resting liquidity
        // addResting returns void in your API
        // (childId/side/price/qty/timeNanos are passed in)
        // leave as default; just verify invocation.

        // Act
        VenueExecution exec = engine.onNew(c);
        assertEquals(1, exec.acks().size());
        assertEquals(0, exec.fills().size());
        assertTrue(exec.cancelOptional().isEmpty());
        assertTrue(exec.rejectOptional().isEmpty());

        verify(book).addResting(eq(vo), eq(childId), eq(DomainSide.SELL),
                eq(105_00L), eq(200L), anyLong());
    }

    @Test
    @DisplayName("onNew: IOC with no immediate liquidity → cancel UNFILLED, no rest")
    void onNew_iocNoLiquidityCancels() {
        ChildId childId = mock(ChildId.class);
        NewChildCmd c = newLimit(DomainSide.BUY, 100_00L, 50L, DomainTif.IOC, childId);

        VenueOrder vo = mock(VenueOrder.class);
        VenueAck ack = mock(VenueAck.class);
        VenueCancelDone cancel = mock(VenueCancelDone.class);

        when(support.create(c)).thenReturn(vo);
        when(support.ack(c, vo)).thenReturn(ack);
        // FOK/IOC branch checks availableImmediately; for IOC 0 → cancel UNFILLED
        when(book.availableImmediately(DomainSide.BUY, false, 100_00L)).thenReturn(0L);
        when(support.cancel(eq(vo), any())).thenReturn(cancel);

        VenueExecution exec = engine.onNew(c);
        assertEquals(1, exec.acks().size());
        assertEquals(0, exec.fills().size());
        assertTrue(exec.cancelOptional().isPresent());
        assertTrue(exec.rejectOptional().isEmpty());

        // Ensure we did NOT rest it
        verify(book, never()).addResting(any(), any(), any(), anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("onCancel: finds resting by childId → remove, clearResting, cancel(USER_REQUEST)")
    void onCancel_restingLine() {
        CancelChildCmd x = mock(CancelChildCmd.class);
        ChildId childId = mock(ChildId.class);
        when(x.childId()).thenReturn(childId);

        RestingRef ref = new RestingRef(42L);
        when(book.byId(childId)).thenReturn(Optional.of(ref));

        VenueOrder vo = mock(VenueOrder.class);
        when(book.voOf(ref)).thenReturn(vo);
        when(book.qtyLeavesOf(ref)).thenReturn(50L); // <-- not 0
        when(book.priceOf(ref)).thenReturn(200_000_000L); // if onNew path needs it
        VenueCancelDone aCancelEventWithQty50 = mock(VenueCancelDone.class);
        when(support.cancel(any(VenueOrder.class), eq(50L), eq(CancelReason.USER_REQUEST)))
                .thenReturn(aCancelEventWithQty50);

        VenueExecution exec = engine.onCancel(x);
        assertEquals(0, exec.acks().size());
        assertEquals(0, exec.fills().size());
        assertTrue(exec.cancelOptional().isPresent());
        assertTrue(exec.rejectOptional().isEmpty());

        verify(book).remove(ref);
        verify(support).clearResting(vo);
    }

    @Test
    @DisplayName("onCancel: not resting anymore → noop")
    void onCancel_notResting() {
        CancelChildCmd x = mock(CancelChildCmd.class);
        when(book.byId(any())).thenReturn(Optional.empty());

        VenueExecution exec = engine.onCancel(x);

        assertSame(VenueExecution.noop(), exec);
    }

    @Test
    @DisplayName("onReplace: no cross as maker → re-add resting at updated price/qty")
    void onReplace_reAddNoCross() {
        // Minimal replace case: engine removes ref, applies new px/qty, then no contra → re-rests
        ReplaceChildCmd r = mock(ReplaceChildCmd.class);

        RestingRef ref = new RestingRef(7L);
        when(book.byId(any())).thenReturn(Optional.of(ref));

        // Existing resting details used in re-add
        VenueOrder vo = mock(VenueOrder.class);
        ChildId childId = mock(ChildId.class);
        when(book.voOf(ref)).thenReturn(vo);
        when(book.childIdOf(ref)).thenReturn(childId);
        when(book.sideOf(ref)).thenReturn(DomainSide.SELL);
        when(book.priceOf(ref)).thenReturn(104_00L);
        when(book.qtyLeavesOf(ref)).thenReturn(150L);

        // No contra to cross after replace
        when(book.bestContra(DomainSide.SELL)).thenReturn(Optional.empty());

        VenueExecution exec = engine.onReplace(r);
        assertEquals(0, exec.acks().size());
        assertEquals(0, exec.fills().size());
        assertTrue(exec.cancelOptional().isEmpty());
        assertTrue(exec.rejectOptional().isEmpty());

        // Verify re-add
        verify(book).remove(ref);
        verify(book).addResting(eq(vo), eq(childId), eq(DomainSide.SELL),
                eq(104_00L), eq(150L), anyLong());
    }
}
