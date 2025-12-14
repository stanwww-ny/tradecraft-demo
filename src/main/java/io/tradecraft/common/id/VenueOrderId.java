package io.tradecraft.common.id;

public record VenueOrderId(String value) implements Identifier {
    public VenueOrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("VenueOrderId cannot be blank");
        }
    }

    public static VenueOrderId of(String value) {
        return new VenueOrderId(value);
    }

    @Override
    public String toString() {
        return value; // cleaner than ParentOrderId[value=...]
    }
}