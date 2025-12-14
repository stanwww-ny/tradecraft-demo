package io.tradecraft.common.id;

public record ChildId(String value) implements Identifier {
    public ChildId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("childId cannot be blank");
        }
    }

    public static ChildId of(String value) {
        return new ChildId(value);
    }

    public static ChildId nextId() {
        return new ChildId(Identifier.join("SOR-CH-", Identifier.uuid32()));
    }

    @Override
    public String toString() {
        return value; // cleaner than ParentOrderId[value=...]
    }
}