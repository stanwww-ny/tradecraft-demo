package io.tradecraft.fixqfj.mapper;

import io.tradecraft.common.domain.instrument.DomainSecurityIdSource;
import io.tradecraft.common.domain.market.DomainOrdType;
import io.tradecraft.common.domain.market.DomainSide;
import io.tradecraft.common.domain.market.DomainTif;
import quickfix.field.OrdType;
import quickfix.field.SecurityIDSource;
import quickfix.field.Side;
import quickfix.field.TimeInForce;

/**
 * FIX <-> domain enum mappings isolated behind a tiny, testable utility.
 * <p>
 * Goals: - Keep SOR/OMS domain enums FIX-agnostic - Centralize char-code conversions (no magic literals sprinkled
 * around) - Provide both strict and lenient helpers
 */
public final class FixOrderMappings {
    private FixOrderMappings() {
    }

    /* ===================== OrderSide ===================== */

    /**
     * Domain -> FIX char
     */
    public static char toFix(final DomainSide side) {
        if (side == null) {
            return Side.UNDISCLOSED;
        }
        return switch (side) {
            case BUY -> Side.BUY;              // '1'
            case SELL -> Side.SELL;             // '2'
            case SELL_SHORT -> Side.SELL_SHORT;       // '5'
            case SELL_SHORT_EXEMPT -> Side.SELL_SHORT_EXEMPT;// '6'
            default -> Side.UNDISCLOSED;
        };
    }

    /**
     * FIX char -> Domain (strict)
     */
    public static DomainSide toDomainSide(final char fixSide) {
        return switch (fixSide) {
            case Side.BUY -> DomainSide.BUY;
            case Side.SELL -> DomainSide.SELL;
            case Side.SELL_SHORT -> DomainSide.SELL_SHORT;
            case Side.SELL_SHORT_EXEMPT -> DomainSide.SELL_SHORT_EXEMPT;

            default -> DomainSide.UNDISCLOSED;
        };
    }

    /* ===================== OrdType ===================== */

    /**
     * Domain -> FIX char
     */
    public static char toFix(final DomainOrdType type) {
        if (type == null) return OrdType.MARKET;
        return switch (type) {
            case MARKET -> OrdType.MARKET;            // '1'
            case LIMIT -> OrdType.LIMIT;             // '2'
            case STOP -> OrdType.STOP_STOP_LOSS;            // '3'
            case STOP_LIMIT -> OrdType.STOP_LIMIT;        // '4'
            case MARKET_ON_CLOSE -> OrdType.MARKET_ON_CLOSE;   // '5'
            case LIMIT_ON_CLOSE -> OrdType.LIMIT_ON_CLOSE;    // 'B'
            case MARKET_IF_TOUCHED -> OrdType.MARKET_IF_TOUCHED; // 'J'
            case LIMIT_IF_TOUCHED -> OrdType.MARKET_WITH_LEFT_OVER_AS_LIMIT;  // 'K'
            case MARKET_TO_LIMIT -> OrdType.COUNTER_ORDER_SELECTION;   // 'Q'
            case PEGGED -> OrdType.PEGGED;            // 'P'
        };
    }

    /**
     * FIX char -> Domain (strict)
     */
    public static DomainOrdType toDomainOrdType(final char fixOrdType) {
        return switch (fixOrdType) {
            case OrdType.MARKET -> DomainOrdType.MARKET;
            case OrdType.LIMIT -> DomainOrdType.LIMIT;
            case OrdType.STOP_STOP_LOSS -> DomainOrdType.STOP;
            case OrdType.STOP_LIMIT -> DomainOrdType.STOP_LIMIT;
            case OrdType.MARKET_ON_CLOSE -> DomainOrdType.MARKET_ON_CLOSE;
            case OrdType.LIMIT_ON_CLOSE -> DomainOrdType.LIMIT_ON_CLOSE;
            case OrdType.MARKET_IF_TOUCHED -> DomainOrdType.MARKET_IF_TOUCHED;
            case OrdType.MARKET_WITH_LEFT_OVER_AS_LIMIT -> DomainOrdType.LIMIT_IF_TOUCHED;
            case OrdType.COUNTER_ORDER_SELECTION -> DomainOrdType.MARKET_TO_LIMIT;
            case OrdType.PEGGED -> DomainOrdType.PEGGED;
            default -> DomainOrdType.MARKET;
        };
    }

    /* ===================== TimeInForce ===================== */

    /**
     * Domain -> FIX char
     */
    public static char toFix(final DomainTif tif) {
        if (tif == null) throw new IllegalArgumentException("OrderTimeInForce is null");
        return switch (tif) {
            case DAY -> TimeInForce.DAY;                       // '0'
            case GTC -> TimeInForce.GOOD_TILL_CANCEL;          // '1'
            case OPG -> TimeInForce.AT_THE_OPENING;            // '2'
            case IOC -> TimeInForce.IMMEDIATE_OR_CANCEL;       // '3'
            case FOK -> TimeInForce.FILL_OR_KILL;              // '4'
            case GTX -> TimeInForce.GOOD_TILL_CROSSING;        // '5'
            case GTD -> TimeInForce.GOOD_TILL_DATE;            // '6'
        };
    }

    /**
     * FIX char -> Domain (strict)
     */
    public static DomainTif toDomainTif(final char fixTif) {
        return switch (fixTif) {
            case TimeInForce.DAY -> DomainTif.DAY;
            case TimeInForce.GOOD_TILL_CANCEL -> DomainTif.GTC;
            case TimeInForce.AT_THE_OPENING -> DomainTif.OPG;
            case TimeInForce.IMMEDIATE_OR_CANCEL -> DomainTif.IOC;
            case TimeInForce.FILL_OR_KILL -> DomainTif.FOK;
            case TimeInForce.GOOD_TILL_CROSSING -> DomainTif.GTX;
            case TimeInForce.GOOD_TILL_DATE -> DomainTif.GTD;
            default -> null;
        };
    }

    public static String toFix(DomainSecurityIdSource src) {
        if (src == null) return null; // or return null if you prefer
        return switch (src) {
            case CUSIP -> SecurityIDSource.CUSIP;            // "1"
            case SEDOL -> SecurityIDSource.SEDOL;            // "2"
            case ISIN -> SecurityIDSource.ISIN_NUMBER;      // "4"
            case RIC -> SecurityIDSource.RIC_CODE;         // "5"
            case SYMBOL -> SecurityIDSource.EXCHANGE_SYMBOL;  // "8"
            case BLOOMBERG -> SecurityIDSource.BLOOMBERG_SYMBOL; // "A"
            case UNKNOWN -> null;                              // "X" (user-defined/custom)
        };
    }
    /* ===================== Null-safe / defaulting helpers ===================== */

    public static char toFixOrDefault(final DomainSide side, final char defaultFix) {
        return (side == null) ? defaultFix : toFix(side);
    }

    public static char toFixOrDefault(final DomainOrdType type, final char defaultFix) {
        return (type == null) ? defaultFix : toFix(type);
    }

    public static char toFixOrDefault(final DomainTif tif, final char defaultFix) {
        return (tif == null) ? defaultFix : toFix(tif);
    }

}
