package io.tradecraft.fixqfj.mapper;

import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import quickfix.Message;
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.LastMkt;
import quickfix.field.LastPx;
import quickfix.field.LastQty;
import quickfix.field.LeavesQty;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.field.OrigClOrdID;
import quickfix.field.SecondaryClOrdID;
import quickfix.field.SecondaryOrderID;
import quickfix.field.SecurityExchange;
import quickfix.field.SecurityID;
import quickfix.field.SecurityIDSource;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.ExecutionReport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Translator between domain DTOs and QuickFIX/J messages. - toExecutionReport: PubExecReport -> FIX 4.4 ExecutionReport
 * - (optional) helpers for NOS mapping in tests
 */
public final class FixOrderTranslator {
    private static final BigDecimal MICROS = new BigDecimal("1000000");
    private static final int DECIMAL_PLACES = 4;
    private final DualTimeSource dualTimeSource;

    public FixOrderTranslator(DualTimeSource dualTimeSource) {
        this.dualTimeSource = dualTimeSource;
    }

    private static double microsToPx(long micros) {
        return micros / 1_000_000.0;
    }

    public static BigDecimal microsToFixPx(long priceMicros) {
        return new BigDecimal(priceMicros).divide(MICROS, DECIMAL_PLACES, RoundingMode.HALF_UP);
    }

    public static BigDecimal microsToFixPx(long priceMicros, int scale) {
        return new BigDecimal(priceMicros).divide(MICROS, scale, RoundingMode.HALF_UP);
    }
    // ---------------- ER outbound ----------------
// in io.tradecraft.fixqfj.mapper.FixOrderTranslator

    public Message toExecutionReport(PubExecReport er) {

        boolean hasTrade = er.lastQty() > 0;

        double avgPxMicros = 0;
        if (er.avgPxMicros() > 0) {
            avgPxMicros = microsToPx(er.avgPxMicros());
        }

        String orderId37 = er.venueOrderId() != null ? er.venueOrderId().value() :
                er.childId() != null ? er.childId().value() :
                        er.clOrdId() != null ? er.clOrdId().value() :
                                "OMS-" + dualTimeSource.nowNanos();

        ExecutionReport m = new ExecutionReport(
                new OrderID(orderId37),
                new ExecID(er.execId().value()),
                new ExecType(ExecAdapters.toExecType(er.status())),
                new OrdStatus(ExecAdapters.toOrdStatus(er.status(), hasTrade)),
                new Side(FixOrderMappings.toFix(er.domainSide())), // null-safe: maps to UNDICLOSED(7)
                new LeavesQty(er.leavesQty()),
                new CumQty(er.cumQty()),
                new AvgPx(avgPxMicros)
        );

        // ---- Identifier fields (now null-safe) ----

        // --- ID echoes ---
        if (er.parentId() != null) {
            m.set(new SecondaryClOrdID(er.parentId().value()));  // 526 (parent)
        }
        if (er.childId() != null) {
            m.set(new SecondaryOrderID(er.childId().value()));   // 198 (child)
        }
        if (er.clOrdId() != null) {
            m.set(new ClOrdID(er.clOrdId().value()));                 // 11 (current)
        }
        if (er.origClOrdId() != null) {
            m.set(new OrigClOrdID(er.origClOrdId().value()));         // 41 (lineage)
        }

        if (er.venueId() != null) {
            //String exDest = VenueMappings.toExDestination(er.venueId());
            String exDest = er.venueId().value();
            if (hasTrade) {
                m.set(new LastMkt(exDest));     // FIX 30
            }
        }
        // ---- Instrument fields (consider guarding if your ERs can be symbol-less) ----
        if (er.instrumentKey() != null) {
            m.set(new Symbol(er.instrumentKey().symbol()));
            if (er.instrumentKey().securityId() != null) {
                m.set(new SecurityID(er.instrumentKey().securityId()));
            }
            String secIdSrc = FixOrderMappings.toFix(er.instrumentKey().securityIdSource());
            if (secIdSrc != null) m.set(new SecurityIDSource(secIdSrc));
            if (er.instrumentKey().mic() != null) m.set(new SecurityExchange(er.instrumentKey().mic()));
        }

        // Prices
        if (hasTrade) {
            m.set(new LastQty(er.lastQty()));
            m.set(new LastPx(microsToPx(er.lastPxMicros())));
        }
        // Timestamp
        m.set(new TransactTime(LocalDateTime.now(ZoneOffset.UTC)));

        return m;
    }


}
