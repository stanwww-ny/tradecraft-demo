package io.tradecraft.common.id;

public record ParentId(String value) implements Identifier {
    public ParentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ParentOrderId cannot be blank");
        }
    }

    public static ParentId of(String value) {
        return new ParentId(value);
    }

    @Override
    public String toString() {
        return value; // cleaner than ParentOrderId[value=...]
    }
}