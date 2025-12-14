// src/main/java/org/example/v4/oms/util/StrictThreadGuard.java
package io.tradecraft.oms.support;

public final class StrictThreadGuard implements ThreadGuard {
    private final String name;
    private volatile Thread owner;

    public StrictThreadGuard(String name) {
        this.name = name;
    }

    @Override
    public void bindToCurrent() {
        owner = Thread.currentThread();
    }

    @Override
    public void assertOwner() {
        Thread t = Thread.currentThread();
        if (t != owner) {
            throw new IllegalStateException(
                    "[" + name + "] wrong thread: current=" + t.getName() +
                            ", owner=" + (owner == null ? "<unbound>" : owner.getName()));
        }
    }

    @Override
    public void clear() {
        owner = null;
    }
}

