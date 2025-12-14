package io.tradecraft.common.domain.market;

// ---------- OrdType mapping (char-based, version-proof) ----------
public enum DomainOrdType {
    MARKET('1'), LIMIT('2'), STOP('3'), STOP_LIMIT('4'),
    MARKET_ON_CLOSE('5'), LIMIT_ON_CLOSE('B'),
    MARKET_IF_TOUCHED('J'), LIMIT_IF_TOUCHED('K'),
    MARKET_TO_LIMIT('Q'), PEGGED('P');
    final char value;

    DomainOrdType(char value) {
        this.value = value;
    }

    public boolean isMarket() {
        return this == MARKET
                || this == MARKET_ON_CLOSE
                || this == MARKET_IF_TOUCHED
                || this == MARKET_TO_LIMIT;
    }

    public boolean isLimit() {
        return this == LIMIT
                || this == LIMIT_ON_CLOSE
                || this == LIMIT_IF_TOUCHED
                || this == PEGGED; // if you want pegged treated as limit
    }
}
