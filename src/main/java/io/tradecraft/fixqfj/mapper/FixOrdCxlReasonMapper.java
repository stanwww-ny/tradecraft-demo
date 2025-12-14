package io.tradecraft.fixqfj.mapper;

import io.tradecraft.common.domain.order.CancelReason;

public final class FixOrdCxlReasonMapper {

    public static int toFix(CancelReason reason) {
        return switch (reason) {
            case USER_REQUEST -> 1;          // Broker/Exchange option
            case TIME_IN_FORCE_EXPIRED -> 0; // Too late to cancel
            case VENUE_HALTED -> 99;         // Other + Text="Trading Halted"
            case RISK_CANCEL -> 99;          // Other + Text="Risk Cancel"
            case ADMIN_CANCEL -> 99;
            case SESSION_LOST -> 2;          // Order already in pending cancel/replace
            case UNFILLED -> 99;
            case OTHER -> 99;
        };
    }

    public static CancelReason fromFix(int code, String text) {
        return switch (code) {
            case 0 -> CancelReason.TIME_IN_FORCE_EXPIRED;
            case 1 -> CancelReason.USER_REQUEST;
            case 2 -> CancelReason.SESSION_LOST;
            default -> {
                if (text != null && text.contains("halt")) yield CancelReason.VENUE_HALTED;
                else if (text != null && text.contains("risk")) yield CancelReason.RISK_CANCEL;
                else yield CancelReason.OTHER;
            }
        };
    }
}
