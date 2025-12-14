package io.tradecraft.fixqfj.mapper;

import io.tradecraft.common.domain.market.ExecKind;
import io.tradecraft.oms.core.OrderStatus;
import quickfix.field.ExecType;
import quickfix.field.OrdStatus;

public class ExecAdapters {
    static ExecKind toKind(ExecType fx) {
        return ExecKindFixMapper.from(fx);
    }

    /**
     * Choose a canonical ExecKind based on the resulting state and whether a trade happened. Use hasTradeDelta=true
     * when LastQty>0 for this message.
     */
    public static char toExecType(OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("OrderStatus is required to map ExecType");
        }
        switch (status) {
            case NEW_PENDING:
                return ExecType.PENDING_NEW;
            case ACKED:
                return ExecType.NEW;      // <-- add
            case NEW:
                return ExecType.NEW;
            case WORKING:
                return ExecType.NEW;
            case PARTIALLY_FILLED:
                return ExecType.PARTIAL_FILL;
            case FILLED:
                return ExecType.FILL;
            case PENDING_CANCEL:
                return ExecType.PENDING_CANCEL;
            case CANCELED:
                return ExecType.CANCELED;
            case PENDING_REPLACE:
                return ExecType.PENDING_REPLACE;
            case REPLACED:
                return ExecType.REPLACED;
            case REJECTED:
                return ExecType.REJECTED;
            case DONE_FOR_DAY:
                return ExecType.DONE_FOR_DAY;
            case EXPIRED:
                return ExecType.EXPIRED;
            case CALCULATED:
                return ExecType.CALCULATED;
            case RESTATED:
                return ExecType.RESTATED;
            case SUSPENDED:
                return ExecType.SUSPENDED;
            default:
                return ExecType.ORDER_STATUS; // catch-all
        }
    }


    public static char toOrdStatus(OrderStatus status, boolean hasTrade) {
        if (status == null) {
            throw new IllegalArgumentException("OrderStatus is required");
        }

        // States that always map to themselves, trade or not
        if (status == OrderStatus.FILLED) return OrdStatus.FILLED;            // 2
        if (status == OrderStatus.PARTIALLY_FILLED) return OrdStatus.PARTIALLY_FILLED;  // 1

        // If there was a trade delta, pre-fill states should present as PartiallyFilled
        if (hasTrade) {
            switch (status) {
                case NEW_PENDING:
                case NEW:
                case ACKED:
                case WORKING:
                    return OrdStatus.PARTIALLY_FILLED; // 1
                // (others like CANCELED/REPLACED/REJECTED shouldn't come with hasTrade=true)
                default:
                    // fall through to non-trade mapping for safety
            }
        }

        // Non-trade (status/restatement/ack) mapping
        switch (status) {
            case NEW_PENDING:
                return OrdStatus.PENDING_NEW;      // A
            case ACKED:
            case NEW:
            case WORKING:
                return OrdStatus.NEW;              // 0
            case PENDING_CANCEL:
                return OrdStatus.PENDING_CANCEL;   // 6
            case CANCELED:
                return OrdStatus.CANCELED;         // 4
            case PENDING_REPLACE:
                return OrdStatus.PENDING_REPLACE;  // E
            case REPLACED:
                return OrdStatus.REPLACED;         // 5
            case REJECTED:
                return OrdStatus.REJECTED;         // 8
            case DONE_FOR_DAY:
                return OrdStatus.DONE_FOR_DAY;     // 3
            case EXPIRED:
                return OrdStatus.EXPIRED;          // C
            case CALCULATED:
                return OrdStatus.CALCULATED;       // B
            // (FILLED / PARTIALLY_FILLED already returned above)
            default:
                throw new IllegalArgumentException("Unsupported OrdStatus mapping for: " + status);
        }
    }


}
