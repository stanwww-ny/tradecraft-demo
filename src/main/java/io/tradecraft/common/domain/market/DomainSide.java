package io.tradecraft.common.domain.market;

// DomainSide.java
public enum DomainSide {
    BUY, SELL, SELL_SHORT, SELL_SHORT_EXEMPT, UNDISCLOSED;

    public boolean isSell() {
        return this == SELL || this == SELL_SHORT || this == SELL_SHORT_EXEMPT;
    }

    public boolean isBuy() {
        return this == BUY;
    }
}

