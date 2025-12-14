package io.tradecraft.fixqfj.mapper;

import io.tradecraft.common.domain.order.RejectReason;

public final class FixOrdRejReasonMapper {

    public static int toFix(RejectReason reason) {
        return switch (reason) {
            case INVALID_INSTRUMENT -> 6;   // OrdRejReason=6 (Invalid Security)
            case INVALID_PRICE -> 18;       // OrdRejReason=18 (Invalid price increment)
            case DUPLICATE_ORDER -> 11;     // OrdRejReason=11 (Duplicate order)
            case RISK_CHECK_FAILED -> 99;   // 99 = Other (with Text="Risk check failed")
            case UNSUPPORTED_ORDER_TYPE -> 4; // OrdRejReason=4 (Unsupported order type)
            case OTHER -> 99;
            default -> 99;
        };
    }

    public static RejectReason fromFix(int code, String text) {
        return switch (code) {
            case 6 -> RejectReason.INVALID_INSTRUMENT;
            case 18 -> RejectReason.INVALID_PRICE;
            case 11 -> RejectReason.DUPLICATE_ORDER;
            case 4 -> RejectReason.UNSUPPORTED_ORDER_TYPE;
            default -> (text != null && text.contains("risk"))
                    ? RejectReason.RISK_CHECK_FAILED
                    : RejectReason.OTHER;
        };
    }
}
