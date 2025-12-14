package io.tradecraft.oms.testutil;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.ParentId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;
import io.tradecraft.common.domain.instrument.InstrumentKey;
import io.tradecraft.common.domain.market.DomainAccountType;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.testing.TestClocks;
import io.tradecraft.fixqfj.session.SessionKey;
import io.tradecraft.oms.event.EvBoundParentNew;
import io.tradecraft.oms.event.EvCancelReq;
import io.tradecraft.oms.event.EvChildAck;
import io.tradecraft.oms.event.EvNew;
import io.tradecraft.oms.event.EvReplaceReq;
import io.tradecraft.util.sample.AccountSamples;
import io.tradecraft.util.sample.ChildIdSamples;
import io.tradecraft.util.sample.ClOrdIdSamples;
import io.tradecraft.util.sample.ExDestSamples;
import io.tradecraft.util.sample.ExecIdSamples;
import io.tradecraft.util.sample.InstrumentKeySamples;
import io.tradecraft.util.sample.ParentIdSamples;
import io.tradecraft.util.sample.SessionKeySamples;
import io.tradecraft.util.sample.VenueIdSamples;
import io.tradecraft.util.sample.VenueOrderIdSamples;

import java.time.Instant;

/**
 * Factory helpers to build OMS events with consistent validation and timestamps. Primarily used in tests.
 */
public final class TestEventFactory {
    private static final DualTimeSource dualTimeSource = TestClocks.msTicker();

    private TestEventFactory() {
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // New Orders
    // ─────────────────────────────────────────────────────────────────────────────

    public static EvBoundParentNew newBoundMarketOrder(
            long nanoTime,
            SessionKey sessionKey,
            ClOrdId clOrdId,
            String accountId,
            DomainAccountType accountType,
            InstrumentKey instrument,
            DomainSide side,
            long qty,
            DomainTif tif,
            String exDest
    ) {
        return new EvBoundParentNew(
                null,
                nanoTime,
                sessionKey,
                clOrdId,
                accountId,
                accountType,
                instrument,
                side,
                qty,
                DomainOrdType.MARKET,
                null,
                tif,
                exDest
        );
    }

    public static EvBoundParentNew newBoundLimitOrder(
            long nanoTime,
            SessionKey sessionKey,
            ClOrdId clOrdId,
            String accountId,
            DomainAccountType accountType,
            InstrumentKey instrument,
            DomainSide side,
            long qty,
            long limitPxMicros,
            DomainTif tif,
            String exDest
    ) {
        return new EvBoundParentNew(
                null,
                nanoTime,
                sessionKey,
                clOrdId,
                accountId,
                accountType,
                instrument,
                side,
                qty,
                DomainOrdType.MARKET,
                limitPxMicros,
                tif,
                exDest
        );
    }

    public static EvNew newMarketOrder(
            ParentId parentId,
            ClOrdId clOrdId,
            String accountId,
            DomainAccountType accountType,
            InstrumentKey instrument,
            DomainSide side,
            long qty,
            DomainTif tif,
            String exDest
    ) {
        return new EvNew(
                parentId,
                dualTimeSource.nowNanos(),
                clOrdId,
                accountId,
                accountType,
                instrument,
                side,
                qty,
                DomainOrdType.MARKET,
                null,
                tif,
                exDest
        );
    }

    public static EvNew newLimitOrder(
            ParentId parentId,
            ClOrdId clOrdId,
            String accountId,
            DomainAccountType accountType,
            InstrumentKey instrument,
            DomainSide side,
            long qty,
            long limitPxMicros,
            DomainTif tif,
            String exDest
    ) {
        return new EvNew(
                parentId,
                dualTimeSource.nowNanos(),
                clOrdId,
                accountId,
                accountType,
                instrument,
                side,
                qty,
                DomainOrdType.LIMIT,
                limitPxMicros,
                tif,
                exDest
        );
    }

    public static EvCancelReq cancelReq(
            ParentId parentId,
            ClOrdId clOrdId,
            ClOrdId origClOrdId,
            String accountId,
            DomainAccountType accountType,
            InstrumentKey instrument,
            DomainSide side,
            Long qty,
            String exDest,
            String reason
    ) {
        return new EvCancelReq(
                parentId,
                dualTimeSource.nowNanos(),
                clOrdId,
                origClOrdId,
                accountId,
                accountType,
                instrument,
                side,
                qty,
                exDest,
                reason
        );
    }

    public static EvReplaceReq replaceReq(
            ParentId parentId,
            ClOrdId clOrdId,
            ClOrdId origClOrdId,
            String accountId,
            DomainAccountType accountType,
            InstrumentKey instrument,
            DomainSide side,
            Long qty,
            DomainOrdType ordType,
            Long limitPxMicros,
            DomainTif tif,
            String exDest
    ) {
        return new EvReplaceReq(
                parentId,
                dualTimeSource.nowNanos(),
                clOrdId,
                origClOrdId,
                accountId,
                accountType,
                instrument,
                side,
                qty,
                ordType,
                limitPxMicros,
                tif,
                exDest
        );
    }

    public static EvChildAck newChildAck(
            long tsNanos,
            ParentId parentId,
            ChildId childId,
            VenueId venueId,
            VenueOrderId venueOrderId,
            ExecId execId,
            DomainTif tif,
            Instant expireAt
    ) {
        return new EvChildAck(
                parentId,
                childId,
                null,
                venueId,
                venueOrderId,
                execId,
                tif,
                expireAt,
                tsNanos
        );
    }
    // ─────────────────────────────────────────────────────────────────────────────
    // Sample objects for unit tests
    // ─────────────────────────────────────────────────────────────────────────────

    public static class Samples {
        private static final ParentId PARENT = new ParentId("P1");
        private static final ClOrdId CLORD = new ClOrdId("C1");
        private static final ClOrdId ORIG_CLORD = new ClOrdId("OC1");
        private static final InstrumentKey INSTR = InstrumentKey.ofSymbol("AAPL");

        public static EvBoundParentNew newBoundMarketOrderAcc1() {
            return TestEventFactory.newBoundMarketOrder(
                    1000_000,
                    SessionKeySamples.SESSION_KEY,
                    ClOrdIdSamples.CL_ORD_ID_001,
                    AccountSamples.ACC1,
                    AccountSamples.ACC1_TYPE,
                    InstrumentKeySamples.AAPL,
                    DomainSide.BUY,
                    100,
                    DomainTif.DAY,
                    ExDestSamples.XNYS
            );
        }

        public static EvBoundParentNew newBoundMarketOrderAcc2() {
            return TestEventFactory.newBoundMarketOrder(
                    2000_000,
                    SessionKeySamples.SESSION_KEY,
                    ClOrdIdSamples.CL_ORD_ID_002,
                    AccountSamples.ACC2,
                    AccountSamples.ACC2_TYPE,
                    InstrumentKeySamples.AAPL,
                    DomainSide.SELL,
                    200,
                    DomainTif.DAY,
                    ExDestSamples.XNYS
            );
        }

        public static EvBoundParentNew newBoundLimitOrderAcc1() {
            return TestEventFactory.newBoundLimitOrder(
                    3000_000,
                    SessionKeySamples.SESSION_KEY,
                    ClOrdIdSamples.CL_ORD_ID_003,
                    AccountSamples.ACC2,
                    AccountSamples.ACC2_TYPE,
                    InstrumentKeySamples.AAPL,
                    DomainSide.SELL,
                    300,
                    220,
                    DomainTif.DAY,
                    ExDestSamples.XNYS
            );
        }

        public static EvNew sampleMarketOrder() {
            return TestEventFactory.newMarketOrder(
                    ParentIdSamples.PARENT_ID_001,
                    ClOrdIdSamples.CL_ORD_ID_001,
                    AccountSamples.ACC1,
                    AccountSamples.ACC1_TYPE,
                    InstrumentKeySamples.AAPL,
                    DomainSide.BUY,
                    100,
                    DomainTif.DAY,
                    ExDestSamples.XNYS
            );
        }

        public static EvNew sampleMarketOrderWithClOrdId(ClOrdId clOrdId) {
            return TestEventFactory.newMarketOrder(
                    ParentIdSamples.PARENT_ID_001,
                    clOrdId,
                    AccountSamples.ACC1,
                    AccountSamples.ACC1_TYPE,
                    InstrumentKeySamples.AAPL,
                    DomainSide.BUY,
                    100,
                    DomainTif.DAY,
                    ExDestSamples.XNYS
            );
        }

        public static EvChildAck sampleChildAck1() {
            return EvChildAck.builder().parentId(ParentIdSamples.PARENT_ID_001)
                    .childId(ChildIdSamples.CHILD_ID_001)
                    .venueId(VenueIdSamples.XNYS)
                    .venueOrderId(VenueOrderIdSamples.V_ID_001)
                    .execId(ExecId.nextId())
                    .tif(DomainTif.DAY)
                    .expireAt(DomainTif.DAY.computeExpireAt(dualTimeSource.nowNanos(), null))
                    .build();
        }

        public static EvChildAck sampleChildAck2() {
            return TestEventFactory.newChildAck(
                    dualTimeSource.nowNanos(),
                    ParentIdSamples.PARENT_ID_002,
                    ChildIdSamples.CHILD_ID_002,
                    VenueIdSamples.XNYS,                           // venueId string (e.g., MIC or gateway id)
                    VenueOrderIdSamples.V_ID_002,
                    ExecIdSamples.EXEC_ID_2,
                    DomainTif.DAY,
                    DomainTif.DAY.computeExpireAt(dualTimeSource.nowNanos(), null)
            );
        }

        public EvNew sampleLimitOrder() {
            return TestEventFactory.newLimitOrder(
                    ParentIdSamples.PARENT_ID_002,
                    ClOrdIdSamples.CL_ORD_ID_999,
                    AccountSamples.ACC1,
                    AccountSamples.ACC1_TYPE,
                    InstrumentKeySamples.AAPL,
                    DomainSide.SELL,
                    200,
                    150_000_000L, // $150 in micros
                    DomainTif.GTC,
                    ExDestSamples.XNYS
            );
        }

        public EvCancelReq sampleCancelReq() {
            return TestEventFactory.cancelReq(
                    PARENT,
                    ClOrdIdSamples.CL_ORD_ID_001,
                    ORIG_CLORD,
                    AccountSamples.ACC1,
                    AccountSamples.ACC1_TYPE,
                    INSTR,
                    DomainSide.BUY,
                    null,
                    ExDestSamples.XNYS,
                    "User requested"
            );
        }

        public EvReplaceReq sampleReplaceReq() {
            return TestEventFactory.replaceReq(
                    PARENT,
                    ClOrdIdSamples.CL_ORD_ID_999,
                    ORIG_CLORD,
                    AccountSamples.ACC1,
                    AccountSamples.ACC1_TYPE,
                    INSTR,
                    DomainSide.SELL,
                    300L,
                    DomainOrdType.LIMIT,
                    151_000_000L, // $151 in micros
                    DomainTif.DAY,
                    ExDestSamples.XNYS
            );
        }
    }
}
