// securityIdSource/main/java/org/example/v4/common/model/Symbol.java
package io.tradecraft.common.model;

public record Symbol(String value) implements EntityIdentifier {
    public Symbol {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Symbol cannot be blank");
    }

    @Override
    public String toString() {
        return value;
    }
}
