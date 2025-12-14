package io.tradecraft.oms;

import org.junit.jupiter.api.Test;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LastShares;
import quickfix.field.LeavesQty;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.SecurityExchange;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.ExecutionReport;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Minimal, self-contained tests: - testLimitFill(): 1000 @ 200.50 → FILLED - testMarketFill(): 1000 @ 200.01 → FILLED
 * <p>
 * What we assert on the FIX ER: 31=LastPx, 32=LastQty, 14=CumQty, 151=LeavesQty, 39=OrdStatus, 150=ExecType And the
 * invariant: origQty = cum + leaves
 */
public class FixExecReportTest {

    /**
     * Builds a FIX 4.4 ExecutionReport with the minimum set of fields you’re sending. This mirrors the lines you showed
     * (39/150/31/32/14/151 plus ids).
     */
    private static ExecutionReport buildFillEr(
            String clOrdId,
            String ordId,
            long origQty,
            long lastQty,
            double lastPx,
            long cumQty,
            long leavesQty
    ) throws Exception {

        ExecutionReport er = new ExecutionReport(
                new OrderID(ordId),
                new ExecID(String.valueOf(System.nanoTime())),
                new ExecType(ExecType.FILL),              // 150
                new OrdStatus(OrdStatus.FILLED),          // 39
                new Side(Side.BUY),                       // 54
                new LeavesQty((int) leavesQty),           // 151 (QuickFIX/J treats Qty as int)
                new CumQty((int) cumQty),                 // 14
                new AvgPx(lastPx)                         // 6
        );

        // Add identifiers
        er.set(new ClOrdID(clOrdId));               // 11
        er.set(new Symbol("AAPL"));                 // 55
        er.set(new OrderQty((int) origQty));        // 38
        er.set(new LastQty((int) lastQty));      // 32
        er.set(new LastPx(lastPx));                 // 31
        er.set(new TransactTime());                 // 60
        er.set(new SecurityExchange("XNAS"));       // 207

        return er;
    }

    @Test
    void testLimitFill() throws Exception {
        final String clOrdId = "CL1";
        final String ordId = "OMS-XYZ-1";
        final long origQty = 1_000L;
        final long lastQty = 1_000L;
        final double lastPx = 200.50;     // $200.50
        final long cumQty = 1_000L;
        final long leaves = 0L;

        ExecutionReport er = buildFillEr(clOrdId, ordId, origQty, lastQty, lastPx, cumQty, leaves);

        // --- numeric tag checks ---
        assertEquals(lastPx, er.getDouble(LastPx.FIELD), 1e-6, "31=LastPx");
        assertEquals(lastQty, er.getInt(LastShares.FIELD), "32=LastQty");
        assertEquals(cumQty, er.getInt(CumQty.FIELD), "14=CumQty");
        assertEquals(leaves, er.getInt(LeavesQty.FIELD), "151=LeavesQty");

        // --- state tags ---
        assertEquals(OrdStatus.FILLED, er.getChar(OrdStatus.FIELD), "39=OrdStatus");
        assertEquals(ExecType.FILL, er.getChar(ExecType.FIELD), "150=ExecType");

        // --- invariant ---
        assertEquals(origQty, cumQty + leaves, "origQty must equal cumQty + leavesQty");
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    @Test
    void testMarketFill() throws Exception {
        final String clOrdId = "CL2";
        final String ordId = "OMS-XYZ-2";
        final long origQty = 1_000L;
        final long lastQty = 1_000L;
        final double lastPx = 200.01;     // e.g., lifted ask
        final long cumQty = 1_000L;
        final long leaves = 0L;

        ExecutionReport er = buildFillEr(clOrdId, ordId, origQty, lastQty, lastPx, cumQty, leaves);

        assertEquals(lastPx, er.getDouble(LastPx.FIELD), 1e-6);
        assertEquals(lastQty, er.getInt(LastShares.FIELD));
        assertEquals(cumQty, er.getInt(CumQty.FIELD));
        assertEquals(leaves, er.getInt(LeavesQty.FIELD));
        assertEquals(OrdStatus.FILLED, er.getChar(OrdStatus.FIELD));
        assertEquals(ExecType.FILL, er.getChar(ExecType.FIELD));
        assertEquals(origQty, cumQty + leaves);
    }
}
