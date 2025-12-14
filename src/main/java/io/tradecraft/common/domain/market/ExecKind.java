package io.tradecraft.common.domain.market;

/**
 * Domain-level execution kinds (no protocol/FIX knowledge).
 */
public enum ExecKind {
    NEW,
    ACK,
    PARTIAL_FILL,
    FILL,
    CANCELED,
    REPLACED,
    REJECTED,
    PENDING_CANCEL,
    PENDING_NEW,
    PENDING_REPLACE,
    ORDER_STATUS // snapshot-only (no event)
}
