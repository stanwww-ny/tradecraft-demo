package io.tradecraft.venue.nbbo;

public class SimpleNbbo implements Nbbo {

    private final long bidPx, askPx, bidSz, askSz;

    SimpleNbbo(long bidPx, long bidSz, long askPx, long askSz) {
        this.bidPx = bidPx;
        this.bidSz = bidSz;
        this.askPx = askPx;
        this.askSz = askSz;
    }

    static long dollars(double px) {
        return Math.round(px * 1_000_000);
    }

    static double dollars(long micros) {
        return micros / 1_000_000.0;
    }

    static SimpleNbbo ofDollars(double bid, long bidSz, double ask, long askSz) {
        return new SimpleNbbo(dollars(bid), bidSz, dollars(ask), askSz);
    }

    public long bidPx() {
        return bidPx;
    }

    public long bidSz() {
        return bidSz;
    }

    public long askPx() {
        return askPx;
    }

    public long askSz() {
        return askSz;
    }
}
