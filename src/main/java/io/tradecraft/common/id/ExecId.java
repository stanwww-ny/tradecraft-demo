package io.tradecraft.common.id;

public record ExecId(String value) implements Identifier {
    public ExecId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ParentOrderId cannot be blank");
        }
    }

    public static ExecId of(String value) {
        return new ExecId(value);
    }

    public static ExecId nextId() {
        return new ExecId(Identifier.join("EX-", Identifier.uuid32()));
    }

    @Override
    public String toString() {
        return value; // cleaner than ParentOrderId[value=...]
    }
}