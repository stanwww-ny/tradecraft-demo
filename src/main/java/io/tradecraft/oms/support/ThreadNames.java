// src/main/java/org/example/v4/oms/util/ThreadNames.java
package io.tradecraft.oms.support;

public final class ThreadNames {
    private ThreadNames() {
    }

    public static String pipeline(int idx) {
        return "pipeline-" + idx;
    }

    public static String erDrainer(int idx) {
        return "er-drainer-" + idx;
    }
}
