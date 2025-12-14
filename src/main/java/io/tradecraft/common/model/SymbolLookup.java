// securityIdSource/main/java/org/example/v4/common/model/SymbolLookup.java
package io.tradecraft.common.model;

/**
 * Type-safe facade over {@link CanonicalLookup} for {@link Symbol}.
 */
public final class SymbolLookup {
    private final Lookup<Symbol> delegate = CanonicalLookup.of(Symbol::new);

    public Symbol lookup(String name) {
        return delegate.lookup(name);
    }

    public int size() {
        return delegate.size();
    }
}
