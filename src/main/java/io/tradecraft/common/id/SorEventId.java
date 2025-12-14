package io.tradecraft.common.id;

public record SorEventId(String value) implements Identifier {
    public SorEventId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SorEventId cannot be blank");
        }
    }

    public static SorEventId of(String value) {
        return new SorEventId(value);
    }

    public static SorEventId nextId() {
        return new SorEventId(Identifier.join("SOR-EV", Identifier.uuid32()));
    }

    @Override
    public String toString() {
        return value; // cleaner than ParentOrderId[value=...]
    }

}