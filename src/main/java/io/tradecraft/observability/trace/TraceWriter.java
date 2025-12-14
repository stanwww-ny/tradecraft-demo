package io.tradecraft.observability.trace;

import io.tradecraft.common.envelope.Envelope;

public interface TraceWriter {
    void write(Envelope<?> env);
    void close() throws Exception;
}
