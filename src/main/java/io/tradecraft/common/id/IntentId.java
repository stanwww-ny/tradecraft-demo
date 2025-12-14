package io.tradecraft.common.id;

public record IntentId(String value) implements Identifier {
    public IntentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("IntentId cannot be blank");
        }
    }

    public static IntentId of(String value) {
        return new IntentId(value);
    }

    @Override
    public String toString() {
        return value; // cleaner than ParentOrderId[value=...]
    }
}