package io.tradecraft.common.domain.validation;

import io.tradecraft.common.domain.market.DomainOrdType;

public final class OrdTypeValidator {

    public static void validate(DomainOrdType ordType, Long limitPxMicros) {
        if (ordType == DomainOrdType.MARKET) {
            if (limitPxMicros != null && limitPxMicros != 0) {
                throw new IllegalArgumentException("MARKET must have null limitPxMicros");
            }
            return;
        }

        if ((ordType == DomainOrdType.LIMIT || ordType == DomainOrdType.STOP_LIMIT)
                && (limitPxMicros == null || limitPxMicros <= 0)) {
            throw new IllegalArgumentException("LIMIT/STOP_LIMIT require limitPxMicros > 0");
        }
    }

}
