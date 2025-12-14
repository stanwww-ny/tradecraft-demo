package io.tradecraft.common.model;

/**
 * Type-safe facade over {@link CanonicalLookup} for {@link ExDest}.
 */
public final class ExDestLookup {
    private final Lookup<ExDest> delegate = CanonicalLookup.of(ExDest::new);

    public ExDest lookup(String name) {
        return delegate.lookup(name);
    }

    public int size() {
        return delegate.size();
    }
}
