package io.tradecraft.bootstrap;

import java.util.List;

public final class Composite implements Lifecycle, AutoCloseable {
    private final List<Lifecycle> parts;

    public Composite(List<Lifecycle> parts) {
        this.parts = parts;
    }

    @Override
    public void start() {
        parts.forEach(Lifecycle::start);
    }

    @Override
    public void stop() { // stop in reverse order
        for (int i = parts.size() - 1; i >= 0; i--)
            try {
                parts.get(i).stop();
            } catch (Throwable ignore) {
            }
    }

    @Override
    public void close() {
        stop();
    }
}