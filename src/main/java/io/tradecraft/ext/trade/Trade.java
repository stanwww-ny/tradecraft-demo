package io.tradecraft.ext.trade;

/**
 * Immutable representation of one CSV row.
 */
public final class Trade {
    public final TradeAction tradeAction;
    public final String account;
    public final String accountType;
    public final String symbol;
    public final String securityId;
    public final String securityIdSrc;
    public final String ric;
    public final String securityType;
    public final String sideText;
    public final long quantity;
    public final String orderTypeText;
    public final Double limitPrice;           // null if not provided
    public final String tifText;
    public final String exDestination;        // optional (FIX tag 100)
    public final String handlingInstruction;  // optional (FIX tag 21)
    public String clOrdId;
    public String origClOrdId;

    public Trade(String account, String symbol, String sideText, long quantity,
                 String orderTypeText, Double limitPrice, String tifText,
                 String exDestination, String handlingInstruction) {
        this.tradeAction = TradeAction.NEW;
        this.account = account;
        this.accountType = "";
        this.symbol = symbol;
        this.securityId = "";
        this.securityIdSrc = "";
        this.ric = "";
        this.securityType = "";
        this.sideText = sideText;
        this.quantity = quantity;
        this.orderTypeText = orderTypeText;
        this.limitPrice = limitPrice;
        this.tifText = tifText;
        this.exDestination = exDestination;
        this.handlingInstruction = handlingInstruction;
    }

    public Trade(String tradeAction, String clOrdId, String origClOrdId, String account, String accountType,
                 String symbol, String securityId, String securityIdSrc, String ric, String securityType,
                 String sideText, long quantity,
                 String orderTypeText, Double limitPrice, String tifText,
                 String exDestination, String handlingInstruction) {
        this.tradeAction = TradeAction.of(tradeAction);
        this.clOrdId = clOrdId;
        this.origClOrdId = origClOrdId;
        this.account = account;
        this.accountType = accountType;
        this.symbol = symbol;
        this.securityId = securityId;
        this.securityIdSrc = securityIdSrc;
        this.ric = ric;
        this.securityType = securityType;
        this.sideText = sideText;
        this.quantity = quantity;
        this.orderTypeText = orderTypeText;
        this.limitPrice = limitPrice;
        this.tifText = tifText;
        this.exDestination = exDestination;
        this.handlingInstruction = handlingInstruction;
    }

    @Override
    public String toString() {
        return "TradeRow{" +
                "account='" + account + '\'' +
                ", symbol='" + symbol + '\'' +
                ", sideText='" + sideText + '\'' +
                ", quantity=" + quantity +
                ", orderTypeText='" + orderTypeText + '\'' +
                ", limitPrice=" + limitPrice +
                ", tifText='" + tifText + '\'' +
                ", exDestination='" + exDestination + '\'' +
                ", handlingInstruction='" + handlingInstruction + '\'' +
                '}';
    }

    // ======= Builder API =======
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String clOrdId;
        private TradeAction tradeAction;
        private String account;
        private String accountType;
        private String symbol;
        private String securityId;
        private String securityIdSrc;
        private String ric;
        private String securityType;
        private String sideText;
        private long quantity;
        private String orderTypeText;
        private Double limitPrice;
        private String tifText;
        private String exDestination;
        private String handlingInstruction;

        private Builder() {}

        public Builder tradeAction(TradeAction tradeAction) {
            this.tradeAction = tradeAction; return this;
        }

        /** Convenience for CSV/string inputs */
        public Builder tradeAction(String tradeAction) {
            if (tradeAction != null) this.tradeAction = TradeAction.valueOf(tradeAction.trim());
            return this;
        }

        public Builder clOrdId(String clOrdId) { this.clOrdId = clOrdId; return this; }
        public Builder account(String account) { this.account = account; return this; }
        public Builder accountType(String accountType) { this.accountType = accountType; return this; }
        public Builder symbol(String symbol) { this.symbol = symbol; return this; }
        public Builder securityId(String securityId) { this.securityId = securityId; return this; }
        public Builder securityIdSrc(String securityIdSrc) { this.securityIdSrc = securityIdSrc; return this; }
        public Builder ric(String ric) { this.ric = ric; return this; }
        public Builder securityType(String securityType) { this.securityType = securityType; return this; }
        public Builder sideText(String sideText) { this.sideText = sideText; return this; }
        public Builder quantity(long quantity) { this.quantity = quantity; return this; }
        public Builder orderTypeText(String orderTypeText) { this.orderTypeText = orderTypeText; return this; }
        public Builder limitPrice(Double limitPrice) { this.limitPrice = limitPrice; return this; }
        public Builder limitPrice(double limitPrice) { this.limitPrice = limitPrice; return this; }
        public Builder tifText(String tifText) { this.tifText = tifText; return this; }
        public Builder exDestination(String exDestination) { this.exDestination = exDestination; return this; }
        public Builder handlingInstruction(String handlingInstruction) { this.handlingInstruction = handlingInstruction; return this; }

        public Trade build() {
            // Minimal validation; extend as needed
            return new Trade(this);
        }
    }

    // Private ctor to materialize from Builder (does not remove existing ctors)
    private Trade(Builder b) {
        this.clOrdId = b.clOrdId;
        this.tradeAction = b.tradeAction;
        this.account = b.account;
        this.accountType = b.accountType;
        this.symbol = b.symbol;
        this.securityId = b.securityId;
        this.securityIdSrc = b.securityIdSrc;
        this.ric = b.ric;
        this.securityType = b.securityType;
        this.sideText = b.sideText;
        this.quantity = b.quantity;
        this.orderTypeText = b.orderTypeText;
        this.limitPrice = b.limitPrice;
        this.tifText = b.tifText;
        this.exDestination = b.exDestination;
        this.handlingInstruction = b.handlingInstruction;
    }


}
