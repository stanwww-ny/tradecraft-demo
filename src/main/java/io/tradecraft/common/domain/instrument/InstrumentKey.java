package io.tradecraft.common.domain.instrument;

public record InstrumentKey(
        String securityId,                // nullable for MVP
        DomainSecurityIdSource securityIdSource,       // nullable/UNKNOWN for MVP
        String symbol,                    // FIX 55 (required)
        String mic                        // from 100/207 if available (nullable)
) {
    public static InstrumentKey ofSymbol(String symbol) {
        return new InstrumentKeyBuilder()
                .symbol(symbol)
                .build();
    }

    public static InstrumentKey ofFix(String symbol, String mic,
                                      String securityId, DomainSecurityIdSource src) {
        return new InstrumentKeyBuilder()
                .symbol(symbol)
                .securityId(securityId)            // nullable OK
                .securityIdSource(src)        // UNKNOWN if null/unmapped
                .mic(mic)                     // nullable OK
                .build();
    }
}
