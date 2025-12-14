package io.tradecraft.common.envelope;

import java.util.function.Consumer;

public interface ReplaySource {
    void play(Consumer<Object> sink);
    void play(Consumer<Object> sink, Consumer<Object> meta);
}
