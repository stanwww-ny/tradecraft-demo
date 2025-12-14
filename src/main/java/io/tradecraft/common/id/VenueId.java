package io.tradecraft.common.id;

import java.util.Map;

public record VenueId(String value) {
    public static final VenueId XNAS = new VenueId("XNAS");
    public static final VenueId NYSE = new VenueId("NYSE");
    public static final VenueId ARCA = new VenueId("ARCA");
    public static final VenueId BATS = new VenueId("BATS");

    public static Map<String, VenueId> names =
            Map.of(XNAS.value, XNAS, NYSE.value, NYSE, ARCA.value, ARCA, BATS.value, BATS);

    public static VenueId of(String value) {
        if (value == null || value.isBlank()) {
            return XNAS;
        }
        if (names.containsKey(value)) {
            return names.get(value);
        }
        return new VenueId(value);
    }
}
