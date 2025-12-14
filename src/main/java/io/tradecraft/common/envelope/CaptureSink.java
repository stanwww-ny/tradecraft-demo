package io.tradecraft.common.envelope;

public interface CaptureSink {
    void capture(Object ev);
    void capture(Object ev, Object meta);
}
