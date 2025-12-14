package io.tradecraft.common.id;

public record ClOrdId(String value) implements Identifier {
    public ClOrdId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ClOrdId cannot be blank");
        }
    }

    public static ClOrdId of(String value) {
        return new ClOrdId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}