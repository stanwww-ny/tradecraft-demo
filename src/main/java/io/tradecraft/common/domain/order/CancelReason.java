package io.tradecraft.common.domain.order;

public enum CancelReason {
    USER_REQUEST,
    TIME_IN_FORCE_EXPIRED,
    VENUE_HALTED,
    RISK_CANCEL,
    ADMIN_CANCEL,
    SESSION_LOST,
    UNFILLED,
    OTHER
}