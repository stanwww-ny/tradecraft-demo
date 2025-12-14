package io.tradecraft.oms.core;

import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.oms.event.EvAck;
import io.tradecraft.oms.event.EvChildAck;
import io.tradecraft.oms.event.EvChildFill;
import io.tradecraft.oms.event.EvNew;
import io.tradecraft.oms.repo.ParentFsmRepository;
import io.tradecraft.oms.runtime.DefaultParentFsmRepository;
import io.tradecraft.oms.runtime.InMemoryParentStateStore;
import io.tradecraft.util.sample.AccountSamples;
import io.tradecraft.util.sample.ChildIdSamples;
import io.tradecraft.util.sample.ClOrdIdSamples;
import io.tradecraft.util.sample.ExDestSamples;
import io.tradecraft.util.sample.ExecIdSamples;
import io.tradecraft.util.sample.InstrumentKeySamples;
import io.tradecraft.util.sample.ParentIdSamples;
import io.tradecraft.util.sample.IntentIdSamples;
import io.tradecraft.util.sample.VenueIdSamples;
import io.tradecraft.util.sample.VenueOrderIdSamples;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates both state transitions AND Effects payloads produced by FSM.apply(). Flow: EvNew -> EvChildAck -> EvAck ->
 * EvChildFill
 */
public class ParentOrderFsmApplyEffectsTest {

    private static final long T0 = 1_000_000_000L;
    private static final long T1 = 1_000_100_000L;
    private static final long T2 = 1_000_200_000L;
    private static final long T3 = 1_000_300_000L;

    private static void assertEffectsNotNull(Effects e) {
        assertNotNull(e, "Effects should not be null");
        assertNotNull(e.newState(), "Effects.newState should not be null");
        assertNotNull(e.execReports(), "Effects.execReports should not be null");
        assertNotNull(e.intents(), "Effects.intents should not be null");
        assertNotNull(e.followUps(), "Effects.followUps should not be null");
    }

    @Test
    void fullCycle_validates_effects_and_state() {
        ParentFsmRepository fsmRepo = new DefaultParentFsmRepository();
        ParentStateStore store = new InMemoryParentStateStore();
        NewStateMapper mapper = new DefaultNewStateMapper();

        ParentId pid = ParentIdSamples.PARENT_ID_001;

        // Build EvNew using samples
        EvNew evNew = new EvNew(
                pid,
                T0,
                ClOrdIdSamples.CL_ORD_ID_001,
                AccountSamples.ACC1,
                AccountSamples.ACC1_TYPE,
                InstrumentKeySamples.AAPL,
                DomainSide.BUY,
                1_000L,
                DomainOrdType.MARKET,
                null,                  // limitPxMicros
                DomainTif.DAY,
                ExDestSamples.XNYS
        );

        // Bootstrap initial state (FSM.onNew expects non-null)
        OrderState s0 = mapper.from(evNew, IntentIdSamples.INTENT_ID);
        store.put(s0);

        ParentOrderFsm fsm = fsmRepo.get(pid);

        // 1) EvNew
        Effects eff1 = fsm.apply(store.get(pid), evNew);
        assertEffectsNotNull(eff1);
        assertTrue(eff1.intents().isEmpty() || eff1.intents().stream().allMatch(Objects::nonNull));
        assertTrue(eff1.execReports().isEmpty() || eff1.execReports().stream().allMatch(Objects::nonNull));
        store.put(eff1.newState());

        OrderState s1 = eff1.newState();
        assertEquals(0L, s1.cumQty());
        assertEquals(1_000L, s1.orderQty());
        assertTrue(s1.status() == OrderStatus.NEW_PENDING || s1.status() == OrderStatus.NEW);

        // 2) EvChildAck
        EvChildAck evChildAck = EvChildAck.builder()
                .parentId(pid).childId(ChildIdSamples.CHILD_ID_001)
                .venueId(VenueIdSamples.XNYS).venueOrderId( VenueOrderIdSamples.V_ID_001)
                .execId(ExecIdSamples.EXEC_ID_1)
                .tsNanos(Instant.ofEpochSecond((T0 / 1_000_000_000L) + 86_400).toEpochMilli()).build();

        Effects eff2 = fsm.apply(store.get(pid), evChildAck);
        assertEffectsNotNull(eff2);
        // Validate either an internal follow-up (EvAck) OR an intent requesting parent-ack
        boolean hasFollowUpAck = eff2.followUps().stream().anyMatch(e -> e instanceof EvAck);
        boolean hasAckIntent = eff2.intents().stream().map(x -> x.getClass().getSimpleName().toLowerCase()).anyMatch(n -> n.contains("ack"));
        assertTrue(hasFollowUpAck || hasAckIntent,
                "after EvChildAck, FSM should either emit a follow-up EvAck or an Ack-intent");

        store.put(eff2.newState());
        OrderState s2 = eff2.newState();
        assertTrue(s2.status() == OrderStatus.NEW || s2.status() == OrderStatus.ACKED || s2.status() == OrderStatus.WORKING || s2.status() == OrderStatus.NEW_PENDING);

        // 3) EvAck (explicit parent-level ack; even if follow-up would have done it, we validate handling)
        EvAck evAck = new EvAck(pid,
                ChildIdSamples.CHILD_ID_001,
                VenueIdSamples.XNYS,
                VenueOrderIdSamples.V_ID_001,
                ExecIdSamples.EXEC_ID_1, T2);
        Effects eff3 = fsm.apply(store.get(pid), evAck);
        assertEffectsNotNull(eff3);
        // Parent ack may trigger an upstream ER(New) and/or planning no-ops; assert types if present
        assertTrue(eff3.execReports().isEmpty() || eff3.execReports().stream().allMatch(Objects::nonNull));
        store.put(eff3.newState());
        OrderState s3 = eff3.newState();
        assertTrue(s3.status() == OrderStatus.ACKED || s3.status() == OrderStatus.WORKING);

        // 4) EvChildFill (full fill)
        EvChildFill evChildFill = new EvChildFill(
                pid,
                T3,
                ChildIdSamples.CHILD_ID_001,
                VenueIdSamples.XNYS,
                VenueOrderIdSamples.V_ID_002,
                ExecIdSamples.EXEC_ID_2,
                1_000L,            // lastQty
                195_000_000L,      // lastPxMicros
                1_000L,            // cumQty after fill
                0,
                true               // final
        );

        Effects eff4 = fsm.apply(store.get(pid), evChildFill);
        assertEffectsNotNull(eff4);
        // On fill we expect at least 1 exec report published
        assertFalse(eff4.execReports().isEmpty(), "final fill should produce an ExecReport");
        store.put(eff4.newState());
        OrderState s4 = eff4.newState();
        assertEquals(1_000L, s4.cumQty());
        assertEquals(0L, s4.leavesQty());
        assertTrue(s4.status() == OrderStatus.FILLED || s4.isDone());
    }
}
