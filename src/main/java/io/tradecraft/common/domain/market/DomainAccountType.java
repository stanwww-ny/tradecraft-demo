package io.tradecraft.common.domain.market;

/**
 * Domain mirror of FIX 581 (AccountType).
 */
public enum DomainAccountType {
    CUSTOMER(1),

    // Your requested names:
    PRIME_BROKERAGE(2),   // FIX often calls this "Firm"/"Non-Customer"
    HOUSE(3),             // aka House Trader
    FLOOR(4),             // aka Floor Trader
    INDIVIDUAL(6),

    // Optional extras you may encounter:
    HOUSE_CROSS_MARGINED(7),
    JOINT_BACK_OFFICE(8),

    UNKNOWN(0);

    private final int fixCode;

    DomainAccountType(int fixCode) {
        this.fixCode = fixCode;
    }
}
