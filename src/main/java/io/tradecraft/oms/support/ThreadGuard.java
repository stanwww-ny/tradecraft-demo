package io.tradecraft.oms.support;

public interface ThreadGuard {
    /**
     * Bind the current thread as the owner (call once at thread start).
     */
    void bindToCurrent();

    /**
     * Assert the current thread is the owner; throw if not.
     */
    void assertOwner();

    /**
     * Optional: clear owner at shutdown/tests.
     */
    default void clear() {
    }
}

