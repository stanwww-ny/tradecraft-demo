package io.tradecraft.util.sample;

import io.tradecraft.ext.FixMessageBuilder;
import io.tradecraft.ext.trade.Trade;
import io.tradecraft.ext.trade.TradeAction;
import quickfix.fix44.NewOrderSingle;

public class TradeSamples {
    public static final Trade TRADE_BUY = Trade.builder().tradeAction(TradeAction.NEW)
            .clOrdId(ClOrdIdSamples.CL_ORD_ID_001.value())
            .exDestination(ExDestSamples.XNYS).limitPrice(100.0).quantity(100)
            .sideText("BUY").orderTypeText("LIMIT")
            .tifText("DAY")
            .symbol("AAPL").build();
    public static final NewOrderSingle NOS_BUY = FixMessageBuilder.newOrderSingle(TRADE_BUY);
}
