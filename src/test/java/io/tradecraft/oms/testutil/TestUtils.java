// src/test/java/org/example/v4/testutil/TestUtils.java
package io.tradecraft.oms.testutil;

import quickfix.SessionSettings;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class TestUtils {

    private TestUtils() {
    }

    public static SessionSettings qfjSettingsFrom(String cfg) throws Exception {
        try (var in = new ByteArrayInputStream(cfg.getBytes(StandardCharsets.UTF_8))) {
            return new SessionSettings(in);
        }
    }

    public static void sleepQuietly(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean anyThreadWithPrefix(String prefix) {
        return Thread.getAllStackTraces().keySet().stream().anyMatch(t -> t.getName().startsWith(prefix));
    }
}
