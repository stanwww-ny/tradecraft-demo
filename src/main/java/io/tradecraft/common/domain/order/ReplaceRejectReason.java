package io.tradecraft.common.domain.order;

/**
 * Reason codes for why a replace (amend) request was rejected by a venue.
 * Mirrors FIX tag 102 (OrdRejReason) but scoped to child replace events.
 */
public enum ReplaceRejectReason {
    UNKNOWN,

    /** The replace request referenced an unknown or invalid order ID. */
    UNKNOWN_ORDER,

    /** The order is in a state that cannot be modified (e.g. already filled or canceled). */
    ORDER_ALREADY_DONE,

    /** The new parameters (price, qty, side, etc.) are invalid or not allowed. */
    INVALID_PARAMETERS,

    /** The venue rejected due to risk, compliance, or credit checks. */
    RISK_REJECT,

    /** The replace was rejected because of throttling, session state, or internal error. */
    VENUE_ERROR,

    /** The venue did not support modification of this order type or instrument. */
    UNSUPPORTED_OPERATION,

    /** Timeout or no response before acknowledgment window expired. */
    TIMEOUT,

    /** Any other or unspecified venue rejectReason. */
    OTHER
}

