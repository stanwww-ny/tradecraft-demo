package io.tradecraft.common.wire;

public final class Wire {
    private Wire() {
    } // no instantiation

    public static final class Side {
        public static final byte BUY = 1;
        public static final byte SELL = 2;
        public static final byte SELL_SHORT = 5;
        public static final byte SELL_SHORT_EXEMPT = 6;
    }

    /**
     * OrdType wire codes. We mirror FIX numerics where possible and assign explicit custom codes for venueId-specific
     * extensions.
     */
    public static final class OrdType {
        public static final byte MARKET = 1;  // FIX 40=1
        public static final byte LIMIT = 2;  // FIX 40=2
        public static final byte STOP = 3;  // FIX 40=3
        public static final byte STOP_LIMIT = 4;  // FIX 40=4
        public static final byte MOC = 5;  // Market On Close (FIX 40=5)

        // FIX extensions encoded as custom numeric wire codes; your FIX adapter
        // will translate these to the proper FIX OrdType values (e.g. 'J','K','P').
        public static final byte MIT = 10; // Market If Touched    (FIX 40=J)
        public static final byte MWL = 11; // Market With Leftover As Limit (FIX 40=K)
        public static final byte PEG = 12; // Pegged               (FIX 40=P)

        // Non-standard but commonly used on equities/venues:
        public static final byte LIT = 13; // Limit If Touched (venueId-specific)
        public static final byte LOC = 14; // Limit On Close   (FIX 40=B)
    }

    public static final class Tif {
        public static final byte DAY = 0;
        public static final byte GTC = 1;
        public static final byte IOC = 3;
        public static final byte FOK = 4;
        public static final byte OPG = 5;
        public static final byte GTX = 6;
        public static final byte GTD = 7;
    }
}