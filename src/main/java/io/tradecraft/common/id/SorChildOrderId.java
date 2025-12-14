package io.tradecraft.common.id;

public record SorChildOrderId(String value) implements Identifier {
    public SorChildOrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SorChildOrderId cannot be blank");
        }
    }

    public static SorChildOrderId of(String value) {
        return new SorChildOrderId(value);
    }

    public static SorChildOrderId nextId(String prefix) {
        return new SorChildOrderId(Identifier.join(prefix, Identifier.uuid32()));
    }

    @Override
    public String toString() {
        return value; // cleaner than ParentOrderId[value=...]
    }
}