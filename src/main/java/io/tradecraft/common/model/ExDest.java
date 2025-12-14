package io.tradecraft.common.model;

public record ExDest(String value) implements EntityIdentifier {
    public ExDest {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("ExDest cannot be blank");
    }

    @Override
    public String toString() {
        return value;
    }
}
