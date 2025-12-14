package io.tradecraft.common.domain.instrument;

public enum DomainSecurityIdSource {
    CUSIP, SEDOL, ISIN, RIC, BLOOMBERG, SYMBOL, UNKNOWN;
    public static DomainSecurityIdSource fromString(String s) {
        if (s == null) return UNKNOWN;

        return switch (s.trim().toUpperCase()) {
            case "CUSIP" -> CUSIP;
            case "SEDOL" -> SEDOL;
            case "ISIN" -> ISIN;
            case "RIC"    -> RIC;
            case "BLOOMBERG"  -> BLOOMBERG;
            case "SYMBOL" -> SYMBOL;
            default       -> UNKNOWN;
        };
    }
}
