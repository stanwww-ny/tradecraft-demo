package io.tradecraft.common.model;

/**
 * Sealed interface for lookup/canonicalization services. Implementations must be thread-safe.
 */
public sealed interface Lookup<T> permits CanonicalLookup {
    /**
     * Return the canonical instance for the provided raw input.
     */
    T lookup(String raw);

    /**
     * Number of cached/known entries (for tests/metrics).
     */
    int size();
}
