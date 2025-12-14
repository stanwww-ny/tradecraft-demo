package io.tradecraft.venue.nbbo;

/**
 * Immutable NBBO snapshot at a point in time (micros-based).
 */
public record NbboSnapshot(
        Long bidPxMicros,   // nullable if no bid
        Long askPxMicros,   // nullable if no ask
        long tsNanos        // capture timestamp
) {

    public boolean isEmpty() {
        return bidPxMicros == null && askPxMicros == null;
    }

    public boolean hasBid() {
        return bidPxMicros != null;
    }

    public boolean hasAsk() {
        return askPxMicros != null;
    }
}