// securityIdSource/main/java/org/example/v4/common/model/Trader.java
package io.tradecraft.common.model;

public record Trader(String value) implements EntityIdentifier {
    public Trader {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Trader cannot be blank");
    }

    @Override
    public String toString() {
        return value;
    }
}
