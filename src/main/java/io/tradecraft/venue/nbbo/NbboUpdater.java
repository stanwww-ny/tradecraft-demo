package io.tradecraft.venue.nbbo;

public interface NbboUpdater {
    void onTopOfBookUpdate(Long bidPxMicros, Long askPxMicros, long recvTsNanos);
}