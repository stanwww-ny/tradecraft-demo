package io.tradecraft.venue.nbbo;

// No DualTimeSource import here

public final class NbboCache implements NbboProvider, NbboUpdater {

    private static final NbboSnapshot EMPTY = new NbboSnapshot(null, null, 0L);

    // Single volatile immutable snapshot for lock-free reads
    private volatile NbboSnapshot snap = EMPTY;

    /**
     * Single-writer update from your market-data pipeline.
     *
     * @param bidPxMicros bid in micros (nullable if missing)
     * @param askPxMicros ask in micros (nullable if missing)
     * @param recvTsNanos MONOTONIC ingest time (from shared dualTimeSource)
     */
    public void onTopOfBookUpdate(Long bidPxMicros, Long askPxMicros, long recvTsNanos) {
        this.snap = new NbboSnapshot(bidPxMicros, askPxMicros, recvTsNanos);
    }

    @Override
    public NbboSnapshot snapshot() {
        return snap; // pure read; no time calls / no policy
    }
}
