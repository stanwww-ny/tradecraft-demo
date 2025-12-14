package io.tradecraft.common.id;

public record ChildClOrdId(String value) implements Identifier {
    public ChildClOrdId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ClOrdId cannot be blank");
        }
    }

    public static ChildClOrdId of(String value) {
        return new ChildClOrdId(value);
    }

    public static ChildClOrdId nextId() {
        return new ChildClOrdId(Identifier.join("C-", Identifier.uuid32()));
    }

    @Override
    public String toString() {
        return value;
    }
}