package io.tradecraft.common.envelope;

import java.util.Optional;

public interface Channel<T> {
    /** Offer an item non-blocking; return false if full. */
    boolean offer(T item);

    /** Take an item (blocking or polling, depending on impl). */
    Optional<T> poll();

    /** Size hint or metrics. */
    default int size() { return 0; }

    /** Close or flush resources (Chronicle, files, etc). */
    default void close() {}
}
