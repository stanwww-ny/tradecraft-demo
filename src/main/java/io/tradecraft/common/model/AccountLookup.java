// securityIdSource/main/java/org/example/v4/common/model/AccountLookup.java
package io.tradecraft.common.model;

/**
 * Type-safe facade over {@link CanonicalLookup} for {@link Account}.
 */
public final class AccountLookup {
    private final Lookup<Account> delegate = CanonicalLookup.of(Account::new);

    public Account lookup(String name) {
        return delegate.lookup(name);
    }

    public int size() {
        return delegate.size();
    }
}
