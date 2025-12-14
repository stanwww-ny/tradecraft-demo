package io.tradecraft.common.domain.market;

import io.tradecraft.common.domain.time.TradingCalendar;

import java.time.Instant;

// DomainTimeInForce.java
public enum DomainTif {
    DAY,           // 0
    GTC,           // 1: Good-Till-Cancel
    OPG,           // 2: At the Opening
    IOC,           // 3
    FOK,           // 4
    GTX,           // 5: Good-Till-Crossing (rarely used)
    GTD;           // 6: Good-Till-Date (needs ExpireDate on FIX)

    public Instant computeExpireAt(long tsNanos, Instant explicitExpireAt) {
        switch (this) {
            case DAY:
                return TradingCalendar.endOfDay(tsNanos); // utility, e.g., 16:00
            case GTC:
                return null; // no expiry
            case IOC, FOK:
                return Instant.ofEpochSecond(0, tsNanos); // immediate
            case GTD:
                // return explicitExpireAt; // must be provided from FIX tag 432
                // for now, treated as DAY.
                return TradingCalendar.endOfDay(tsNanos); // utility, e.g., 16:00
            default:
                return null;
        }
    }

    public boolean isIOC() {
        return this == IOC;
    }

    public boolean isFOK() {
        return this == FOK;
    }

    public boolean isDAY() {
        return this == DAY;
    }

    public boolean isGTC() {
        return this == GTC;
    }
}

