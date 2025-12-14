package io.tradecraft.common.domain.instrument;

public final class InstrumentKeyBuilder {
    private String securityId;
    private DomainSecurityIdSource securityIdSource = DomainSecurityIdSource.UNKNOWN;
    private String symbol;
    private String mic;

    public InstrumentKeyBuilder securityId(String securityId) {
        this.securityId = securityId;
        return this;
    }

    public InstrumentKeyBuilder securityIdSource(DomainSecurityIdSource src) {
        this.securityIdSource = (src == null ? DomainSecurityIdSource.UNKNOWN : src);
        return this;
    }

    public InstrumentKeyBuilder symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public InstrumentKeyBuilder mic(String mic) {
        this.mic = mic;
        return this;
    }

    public InstrumentKey build() {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalStateException("InstrumentKey requires a symbol (FIX 55).");
        }
        return new InstrumentKey(securityId, securityIdSource, symbol, mic);
    }
}

