package io.tradecraft.common.id;

public record SorOrderId(String value) implements Identifier {
    public SorOrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SorOrderId cannot be blank");
        }
    }

    public static SorOrderId of(String value) {
        return new SorOrderId(value);
    }

    public static SorOrderId nextId() {
        return new SorOrderId(Identifier.join("SOR-ORD-", Identifier.uuid32()));
    }

    @Override
    public String toString() {
        return value; // cleaner than ParentOrderId[value=...]
    }
}