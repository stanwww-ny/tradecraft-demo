package io.tradecraft.common.utils;

import java.util.Map;

public class PriceUtils {
    static final Map<String, Double> TICKS = Map.of(
            "AAPL", 0.01, "MSFT", 0.01, "AMZN", 0.01
    );

    public static double tickOf(String symbol) {
        return TICKS.getOrDefault(symbol, 0.01);
    }

    public static double roundToTick(double px, double tick) {
        return Math.round(px / tick) * tick;
    }
}
