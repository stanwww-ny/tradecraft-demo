package io.tradecraft.sor.state;

/**
 * Lifecycle of a venueId child order driven by SOR/venueId events.
 * NEW_PENDING --(VenueAck)--> ACKED
 * ACKED --(VenueFill, leaves>0)--> PARTIALLY_FILLED
 * ACKED --(VenueFill, leaves==0)--> FILLED
 * PARTIALLY_FILLED --(VenueFill, leaves>0)--> PARTIALLY_FILLED
 * PARTIALLY_FILLED --(VenueFill, leaves==0)--> FILLED
 * --(CancelChildIntent)--> PENDING_CANCEL (only if not terminal already)
 * PENDING_CANCEL --(VenueCancelDone)--> CANCELED
 * NEW_PENDING --(VenueReject)--> REJECTED
 * --(Expiry/TIF)--> EXPIRED

 Idempotency: repeated ACK in ACKED = no-op; repeated CancelDone in CANCELED = no-op
 */
public enum ChildStatus {
    /**
     * Created locally, not yet acked by venueId.
     */
    NEW_PENDING(false, 0),

    /**
     * Venue acknowledged (live on book or working).
     */
    ACKED(true, 10),

    /**
     * At least one fill, but not done.
     */
    PARTIALLY_FILLED(true, 20),

    /**
     * Fully executed. Terminal.
     */
    FILLED(false, 100, true),

    /**
     * Pending Cancel by venueId/user. Terminal.
     */
    PENDING_CANCEL(false, 100, true),

    /**
     * Canceled by venueId/user. Terminal.
     */
    CANCELED(false, 100, true),

    PENDING_REPLACE(false, 100, true),

    /**
     * Explicit venueId reject (e.g., risk/invalid). Terminal.
     */
    REJECTED(false, 100, true),

    /**
     * Expired due to TIF (IOC/FOK failure, DAY end, GTT timeout). Terminal.
     */
    EXPIRED(false, 100, true),

    WORKING(false, 100, true);

    private final boolean live;     // eligible to receive fills/cancels
    private final int rank;     // monotonic progression guard
    private final boolean terminal; // no further transitions allowed

    ChildStatus(boolean live, int rank) {
        this(live, rank, false);
    }

    ChildStatus(boolean live, int rank, boolean terminal) {
        this.live = live;
        this.rank = rank;
        this.terminal = terminal;
    }

    /**
     * True if order is currently working/live at the venueId.
     */
    public boolean isLive() {
        return live;
    }

    /**
     * True if no more state changes are allowed.
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * For guarding monotonic transitions (NEW < ACKED < PARTIALLY_FILLED < ...).
     */
    public int rank() {
        return rank;
    }
}
