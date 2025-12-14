package io.tradecraft.ext.trade;

public enum TradeAction {
    NEW, CANCEL, REPLACE;

    public static TradeAction of(String action) {
        if (TradeAction.NEW.name().equalsIgnoreCase(action)) {
            return TradeAction.NEW;
        }
        if (TradeAction.CANCEL.name().equalsIgnoreCase(action)) {
            return TradeAction.CANCEL;
        }
        if (TradeAction.REPLACE.name().equalsIgnoreCase(action)) {
            return TradeAction.REPLACE;
        }
        return TradeAction.NEW;
    }
}
