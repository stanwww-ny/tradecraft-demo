// securityIdSource/main/java/org/example/v4/common/model/TraderLookup.java
package io.tradecraft.common.model;

/**
 * Type-safe facade over {@link CanonicalLookup} for {@link Trader}.
 */
public final class TraderLookup {
    private final Lookup<Trader> delegate = CanonicalLookup.of(Trader::new);

    public Trader lookup(String name) {
        return delegate.lookup(name);
    }

    public int size() {
        return delegate.size();
    }
}
