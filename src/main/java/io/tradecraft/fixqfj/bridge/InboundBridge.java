package io.tradecraft.fixqfj.bridge;

import io.tradecraft.fixqfj.event.FixEvInbound;

public interface InboundBridge {
    /**
     * Non-blocking offer; returns false if full.
     */
    boolean offer(FixEvInbound event);
}