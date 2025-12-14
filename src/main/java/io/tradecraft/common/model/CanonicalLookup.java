// securityIdSource/main/java/org/example/v4/common/model/CanonicalLookup.java
package io.tradecraft.common.model;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Generic, thread-safe canonicalizer that trims/uppercases a String key and caches value objects.
 */
public final class CanonicalLookup<T> implements Lookup<T> {
    private final Map<String, T> cache = new ConcurrentHashMap<>();
    private final Function<String, T> factory;
    private final UnaryOperator<String> normalizer;

    private CanonicalLookup(Function<String, T> factory, UnaryOperator<String> normalizer) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer");
    }

    /**
     * Default: trim + uppercase normalization.
     */
    public static <T> CanonicalLookup<T> of(Function<String, T> factory) {
        return new CanonicalLookup<>(factory, s -> {
            if (s == null) throw new IllegalArgumentException("name cannot be null");
            String t = s.trim();
            if (t.isEmpty()) throw new IllegalArgumentException("name cannot be blank");
            return t.toUpperCase();
        });
    }

    /**
     * Custom normalizer hook (e.g., venueId-specific instrumentKey rules).
     */
    public static <T> CanonicalLookup<T> of(Function<String, T> factory, UnaryOperator<String> normalizer) {
        return new CanonicalLookup<>(factory, normalizer);
    }

    @Override
    public T lookup(String raw) {
        String key = normalizer.apply(raw);
        return cache.computeIfAbsent(key, factory);
    }

    @Override
    public int size() {
        return cache.size();
    }
}
