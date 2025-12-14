// src/main/java/org/example/v4/oms/util/NoopThreadGuard.java
package io.tradecraft.oms.support;

public enum NoopThreadGuard implements ThreadGuard {
    INSTANCE;

    public void bindToCurrent() {
    }

    public void assertOwner() {
    }
}
