package io.tradecraft.venue.store;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.order.CancelReason;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.matching.orderbook.FillSource;
import io.tradecraft.venue.model.VenueOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for InMemoryVenueOrderRepository
 * <p>
 * Coverage: - create(...) idempotency on ChildId and reverse index by VenueOrderId - get(...) and byVenue(...)
 * symmetric lookups - ack(...) no-op (does not break storage) - applyFill(...) updates cum/leaves - applyReplace(...)
 * updates qty/leaves/limitPx - cancel(...) removes both primary and reverse mapping - seenCmd/seenExec idempotency
 * guards
 */
class InMemoryVenueOrderRepositoryOperationTest {

    private final DualTimeSource dualTimeSource = TestClocks.msTicker();
    private InMemoryVenueOrderRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryVenueOrderRepository(dualTimeSource);
    }

    // --- helpers -------------------------------------------------------------

    private NewChildCmd newCmd(ChildId childId,
                               DomainSide side,
                               DomainOrdType ordType,
                               long qty,
                               Long limitPxMicros) {
        NewChildCmd cmd = mock(NewChildCmd.class, RETURNS_DEEP_STUBS);
        when(cmd.childId()).thenReturn(childId);
        when(cmd.side()).thenReturn(side);
        when(cmd.ordType()).thenReturn(ordType);
        when(cmd.qty()).thenReturn(qty);
        // Many code paths check limit via either priceMicros() or limitPxMicros(); stub both to be safe.
        try {
            when(cmd.priceMicros()).thenReturn(limitPxMicros);
        } catch (Throwable ignore) {
        }
        try {
            when(cmd.priceMicros()).thenReturn(limitPxMicros);
        } catch (Throwable ignore) {
        }
        // Add any other getters your VenueOrder.from(...) requires here:
        // when(cmd.parentId()).thenReturn(new ParentId("P1")); etc.
        return cmd;
    }

    private VenueOrder getByChild(ChildId c) {
        return repo.get(c).orElseThrow();
    }

    // --- create / lookup -----------------------------------------------------

    @Test
    void create_isIdempotentPerChild_andPopulatesReverseIndex() {
        ChildId c1 = new ChildId("C1");
        VenueId v1 = new VenueId("V1");
        VenueOrderId vo1 = new VenueOrderId("101L");

        var cmd = newCmd(c1, DomainSide.BUY, DomainOrdType.LIMIT, 100L, 100_00L);

        VenueOrder first = repo.create(cmd, v1, vo1);
        assertNotNull(first);

        // Creating again with same child but a different generated VOI should *not* overwrite
        VenueOrderId voOther = new VenueOrderId("999L");
        VenueOrder second = repo.create(cmd, v1, voOther);

        assertSame(first, second, "create must be idempotent for the same ChildId");

        // reverse index -> points to the stored VO (vo1), not the new one
        Optional<VenueOrder> byVo1 = repo.byVenue(vo1);
        assertTrue(byVo1.isPresent(), "reverse index should exist for first VOI");
        assertSame(first, byVo1.get());

        // reverse index should NOT map to voOther (since the second create returned existing row)
        assertTrue(repo.byVenue(voOther).isEmpty(), "no reverse mapping should exist for redundant VOI");
    }

    @Test
    void get_and_byVenue_symmetricAfterCreate() {
        ChildId c1 = new ChildId("C1");
        VenueId v1 = new VenueId("V1");
        VenueOrderId vo1 = new VenueOrderId("111L");

        var cmd = newCmd(c1, DomainSide.SELL, DomainOrdType.LIMIT, 50L, 101_00L);

        VenueOrder stored = repo.create(cmd, v1, vo1);

        assertSame(stored, getByChild(c1));
        assertSame(stored, repo.byVenue(vo1).orElseThrow());
    }

    // --- ack / fill / replace transitions -----------------------------------

    @Test
    void ack_isNoopButKeepsRowIntact() {
        ChildId c1 = new ChildId("C1");
        VenueId v1 = new VenueId("V1");
        VenueOrderId vo1 = new VenueOrderId("222L");
        var cmd = newCmd(c1, DomainSide.BUY, DomainOrdType.LIMIT, 10L, 100_00L);

        VenueOrder stored = repo.create(cmd, v1, vo1);

        repo.ack(stored, /*tsNanos*/ 12345L);

        // Row remains accessible
        assertSame(stored, getByChild(c1));
        assertSame(stored, repo.byVenue(vo1).orElseThrow());
    }

    @Test
    void applyFill_updatesCumAndLeaves() {
        ChildId c1 = new ChildId("C1");
        VenueId v1 = new VenueId("V1");
        VenueOrderId vo1 = new VenueOrderId("333L");
        var cmd = newCmd(c1, DomainSide.BUY, DomainOrdType.LIMIT, 100L, 100_00L);

        VenueOrder row = repo.create(cmd, v1, vo1);

        // Fill 30 @ 100
        repo.applyFill(row, 30L, 100_00L, false, FillSource.MATCHING_ENGINE);

        VenueOrder after = getByChild(c1);
        assertEquals(30L, after.cumQty(), "cumQty must increment");
        assertEquals(70L, after.leavesQty(), "leaves must reduce");
        assertEquals(row.qty(), after.qty(), "original qty unchanged");
        assertEquals(row.limitPxMicros(), after.limitPxMicros(), "limit stays the same");

        // Final fill 70 @ 100
        repo.applyFill(after, 70L, 100_00L, true, FillSource.MATCHING_ENGINE);
        VenueOrder done = getByChild(c1);
        assertEquals(100L, done.cumQty());
        assertEquals(0L, done.leavesQty());
    }

    @Test
    void applyReplace_changesQtyAndOrLimit_andBoundsLeaves() {
        ChildId c1 = new ChildId("C1");
        VenueId v1 = new VenueId("V1");
        VenueOrderId vo1 = new VenueOrderId("444L");
        var cmd = newCmd(c1, DomainSide.SELL, DomainOrdType.LIMIT, 100L, 101_00L);

        VenueOrder row = repo.create(cmd, v1, vo1);
        // Pretend we already filled 40 elsewhere:
        repo.applyFill(row, 40L, 101_00L, false, FillSource.MATCHING_ENGINE);
        VenueOrder afterFill = getByChild(c1);
        assertEquals(60L, afterFill.leavesQty());

        // Replace to reduce qty to 70 and change limit to 102.00
        repo.applyReplace(afterFill, /*newQty*/ 70L, /*newLimit*/ 102_00L);
        VenueOrder afterReplace = getByChild(c1);

        assertEquals(70L, afterReplace.qty());
        assertEquals(40L, afterReplace.cumQty());          // cum unchanged by replace
        assertEquals(30L, afterReplace.leavesQty());       // leaves bounded: newQty - cum
        assertEquals(102_00L, afterReplace.limitPxMicros());

        // Replace without new limit: leave limit unchanged
        repo.applyReplace(afterReplace, 80L, /*newLimit*/ null);
        VenueOrder afterReplace2 = getByChild(c1);
        assertEquals(80L, afterReplace2.qty());
        assertEquals(102_00L, afterReplace2.limitPxMicros());
    }

    // --- cancel removes both maps -------------------------------------------

    @Test
    void cancel_removesPrimaryAndReverse() {
        ChildId c1 = new ChildId("C1");
        VenueId v1 = new VenueId("V1");
        VenueOrderId vo1 = new VenueOrderId("555L");
        var cmd = newCmd(c1, DomainSide.SELL, DomainOrdType.LIMIT, 10L, 101_00L);

        VenueOrder row = repo.create(cmd, v1, vo1);
        assertTrue(repo.byVenue(vo1).isPresent());

        repo.cancel(row, CancelReason.USER_REQUEST);

        assertTrue(repo.get(c1).isEmpty(), "primary row must be removed");
        assertTrue(repo.byVenue(vo1).isEmpty(), "reverse mapping must be removed");
    }

    // --- idempotency guards --------------------------------------------------

    @Test
    void seenCmd_and_seenExec_returnTrueOnlyOnFirstSight() {
        assertTrue(repo.seenCmd("x"));
        assertFalse(repo.seenCmd("x"));

        assertTrue(repo.seenExec("y"));
        assertFalse(repo.seenExec("y"));
    }
}
