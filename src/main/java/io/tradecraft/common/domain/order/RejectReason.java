package io.tradecraft.common.domain.order;

public enum RejectReason {
    INVALID_INSTRUMENT,
    INVALID_PRICE,
    DUPLICATE_ORDER,
    RISK_CHECK_FAILED,
    UNSUPPORTED_ORDER_TYPE,
    THROTTLED,
    MALFORMED,
    OTHER
}
