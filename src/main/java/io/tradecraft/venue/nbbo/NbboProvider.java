package io.tradecraft.venue.nbbo;

public interface NbboProvider {
    NbboSnapshot snapshot();

    void onTopOfBookUpdate(Long bidPxMicros, Long askPxMicros, long recvTsNanos);
}
