package io.tradecraft.bootstrap;

import io.tradecraft.venue.nbbo.NbboSnapshot;
import io.tradecraft.venue.nbbo.NbboUpdater;

public final class MarketDataModule {

    private final NbboUpdater nbboUpdater;

    public MarketDataModule(NbboUpdater nbboUpdater) {
        this.nbboUpdater = nbboUpdater;
    }

    public void onTopOfBook(long bidPxMicros, long askPxMicros, long nowNanos) {
        nbboUpdater.onTopOfBookUpdate(bidPxMicros, askPxMicros, nowNanos);
    }

    public void onSnapshot(NbboSnapshot snap, long nowNanos) {
        nbboUpdater.onTopOfBookUpdate(snap.bidPxMicros(), snap.askPxMicros(), nowNanos);
    }
}
