// securityIdSource/main/java/org/example/v4/common/model/EntityIdentifier.java
package io.tradecraft.common.model;

/**
 * Common contract for non-order identifiers: Symbol, Account, Trader, Venue.
 */
public sealed interface EntityIdentifier permits Symbol, Account, Trader, ExDest {
    /**
     * Canonical string value (already normalized by Lookup).
     */
    String value();

    /**
     * Convenience for logs/encoders.
     */
    default String asString() {
        return value();
    }
}