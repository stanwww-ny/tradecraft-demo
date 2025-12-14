package io.tradecraft.ext;

import io.tradecraft.common.domain.instrument.DomainSecurityIdSource;
import quickfix.field.OrdType;
import quickfix.field.SecurityIDSource;
import quickfix.field.SecurityType;
import quickfix.field.Side;
import quickfix.field.TimeInForce;

/**
 * Maps human-readable inputs to FIX field values.
 */
public final class FixFieldMapper {
    private FixFieldMapper() {
    }

    public static char toFixSide(String s) {
        String v = s.trim().toUpperCase();
        return switch (v) {
            case "1", "BUY", "B" -> Side.BUY;
            case "2", "SELL", "S" -> Side.SELL;
            default -> throw new IllegalArgumentException("Unsupported side: " + s);
        };
    }

    public static char toFixOrdType(String s) {
        String v = s.trim().toUpperCase();
        return switch (v) {
            case "1", "MARKET", "MKT" -> OrdType.MARKET;
            case "2", "LIMIT", "LMT" -> OrdType.LIMIT;
            default -> throw new IllegalArgumentException("Unsupported ordType: " + s);
        };
    }

    public static char toFixTif(String s) {
        String v = s.trim().toUpperCase();
        return switch (v) {
            case "0", "DAY" -> TimeInForce.DAY;
            // Extend if needed: "1"/"GTC", "3"/"IOC", "4"/"FOK"
            default -> TimeInForce.DAY;
        };
    }

    public static char toFixHandlInst(String s) {
        String v = s.trim().toUpperCase();
        return switch (v) {
            case "1", "AUTO_PRIVATE" -> '1'; // Automated, private
            case "2", "AUTO_PUBLIC" -> '2'; // Automated, public
            case "3", "MANUAL" -> '3'; // Manual
            default -> '1';
        };
    }

    public static String toFixSecurityIDSource(String s) {
        DomainSecurityIdSource src = DomainSecurityIdSource.fromString(s);
        return switch (src) {
            case CUSIP -> SecurityIDSource.CUSIP;
            case SYMBOL -> SecurityIDSource.EXCHANGE_SYMBOL; // "8"
            case BLOOMBERG   -> SecurityIDSource.BLOOMBERG_SYMBOL;         // "A"
            case ISIN   -> SecurityIDSource.ISIN_NUMBER;     // "4"
            case RIC    -> SecurityIDSource.RIC_CODE;        // "5"
            default -> throw new IllegalArgumentException("Unsupported securityIdSrc: " + src);
        };
    }

    public static String toFixSecurityType(String t) {
        return switch (t.trim().toUpperCase()) {
            case "CS", "EQUITY", "STOCK"   -> SecurityType.COMMON_STOCK; // "CS"
            case "OPT", "OPTION"          -> SecurityType.OPTION;        // "OPT"
            case "FUT", "FUTURE"          -> SecurityType.FUTURE;        // "FUT"
            default                        -> t; // assume already FIX-correct
        };
    }
}
