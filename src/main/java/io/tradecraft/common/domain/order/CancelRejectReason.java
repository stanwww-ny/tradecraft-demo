package io.tradecraft.common.domain.order;

/**
 * Enumerates possible reasons a venueId may reject a cancel or replace request. Mirrors FIX tag 102 (CxlRejReason)
 * values.
 * <p>
 * Typical use: - Cancel request too late (already filled or pending execution) - Unknown or stale order - Venue /
 * gateway rejected the modification
 */
public enum CancelRejectReason {

    /**
     * The order has already fully filled, so cancellation cannot be processed.
     */
    ALREADY_FILLED,

    /**
     * The referenced order ID is unknown or no longer active.
     */
    UNKNOWN_ORDER,

    /**
     * Too late to cancel — e.g., the order is in finalization at the venueId.
     */
    TOO_LATE_TO_CANCEL,

    /**
     * The cancel or replace was rejected explicitly by the venueId.
     */
    REJECTED_BY_VENUE,

    /**
     * The cancel or replace request was malformed or logically invalid.
     */
    INVALID_REQUEST,

    /**
     * Fallback category for unexpected reasons or mapping failures.
     */
    OTHER;

    /**
     * Maps a FIX CxlRejReason code to a CancelRejectReason enum. (Optional helper for mapping FIX messages to internal
     * events.)
     */
    public static CancelRejectReason fromFixCode(int fixCode) {
        return switch (fixCode) {
            case 1 -> TOO_LATE_TO_CANCEL;   // CxlRejReason=1
            case 2 -> UNKNOWN_ORDER;        // CxlRejReason=2
            case 3 -> ALREADY_FILLED;       // CxlRejReason=3
            case 4 -> REJECTED_BY_VENUE;    // CxlRejReason=4 (exchange)
            case 5 -> INVALID_REQUEST;      // CxlRejReason=5 (other)
            default -> OTHER;
        };
    }
}
