package io.tradecraft.oms.core;

public enum OrderStatus {
    NEW_PENDING,
    NEW,            // accepted by OMS, not yet venueId-acked
    ACKED,          // venueId acknowledged (working)
    WORKING,
    PARTIALLY_FILLED,
    FILLED,
    PENDING_CANCEL,
    CANCELED,
    PENDING_REPLACE,
    REPLACED,
    REJECTED,
    DONE_FOR_DAY,
    EXPIRED,
    CALCULATED,
    RESTATED,
    SUSPENDED;

    public static boolean isDone(OrderStatus status) {
        return status == OrderStatus.FILLED
                || status == OrderStatus.CANCELED
                || status == OrderStatus.REJECTED
                || status == OrderStatus.EXPIRED;
    }
}
