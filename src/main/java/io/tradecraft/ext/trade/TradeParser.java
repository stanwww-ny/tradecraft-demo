package io.tradecraft.ext.trade;

import java.io.BufferedReader;
import java.util.List;

public interface TradeParser {
    List<Trade> load(String path, ClassLoader cl);

    List<Trade> parse(BufferedReader br) throws Exception;
}
