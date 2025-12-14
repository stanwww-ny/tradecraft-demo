package io.tradecraft.common.wire;

public record ChildOrderDto(
        String childId,
        String parentOrderId,
        String clOrdId,
        String venue,          // e.g., XNAS, ARCA
        long qty,
        Long pxMicros,       // nullable if market order
        byte ordType,        // use Wire.OrdType
        byte tif,            // use Wire.Tif
        long createdTsNanos,
        int schemaVersion
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
}
