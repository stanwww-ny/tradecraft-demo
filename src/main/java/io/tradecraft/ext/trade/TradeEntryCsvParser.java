package io.tradecraft.ext.trade;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TradeEntryCsvParser implements TradeParser {
    public List<Trade> load(String path, ClassLoader cl) {
        try {
            Path fs = Path.of(path);
            if (Files.exists(fs)) {
                try (BufferedReader br = Files.newBufferedReader(fs)) {
                    return parse(br);
                }
            }
            var in = (cl != null ? cl : TradeCsvParser.class.getClassLoader()).getResourceAsStream(path);
            if (in == null) {
                System.err.println("Trades file not found (FS or resources): " + path);
                return List.of();
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                return parse(br);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Parse the CSV contents into TradeRow objects.
     */
    public List<Trade> parse(BufferedReader br) throws Exception {
        List<Trade> rows = new ArrayList<>();
        String header = br.readLine();
        if (header == null) return rows;

        String line;
        int rowNum = 1;
        while ((line = br.readLine()) != null) {
            rowNum++;
            if (line.isBlank()) continue;
            String[] c = line.split(",");
            for (int i = 0; i < c.length; i++) c[i] = c[i].trim();
            int i = 0;
            String enable = c.length > i ? c[i] : "";
            i++;
            String tradeAction = c.length > i ? c[i] : "";
            i++;
            String clOrdId = c.length > i ? c[i] : "";
            i++;
            String origClOrdId = c.length > i ? c[i] : "";
            i++;
            String account = c.length > i ? c[i] : "";
            i++;
            String accountType = c.length > i ? c[i] : "";
            i++;
            String symbol = c.length > i ? c[i] : "";
            i++;
            String securityId = c.length > i ? c[i] : "";
            i++;
            String securityIdSrc = c.length > i ? c[i] : "";
            i++;
            String ric = c.length > i ? c[i] : "";
            i++;
            String securityType = c.length > i ? c[i] : "";
            i++;
            String side = c.length > i ? c[i] : "";
            i++;
            long qty = c.length > i && !c[i].isBlank() ? Long.parseLong(c[i]) : 0L;
            i++;
            String ordType = c.length > i ? c[i] : "";
            i++;
            String px = c.length > i ? c[i] : "";
            i++;
            String tif = c.length > i ? c[i] : "";
            i++;
            String exDest = c.length > i ? c[i] : "";
            i++;
            String handl = c.length > i ? c[i] : "";
            i++;

            if (!"Y".equalsIgnoreCase(enable)) {
                continue;
            }
            Double limit = (px == null || px.isBlank()) ? null : Double.parseDouble(px);

            if (account.isEmpty() || symbol.isEmpty() || side.isEmpty() || ordType.isEmpty() || tif.isEmpty()) {
                System.err.println("Skipping row " + rowNum + " (missing required fields): " + line);
                continue;
            }
            rows.add(new Trade(tradeAction, clOrdId, origClOrdId, account, accountType,
                    symbol, securityId, securityIdSrc, ric, securityType,
                    side, qty, ordType, limit, tif, exDest, handl));
        }
        System.out.println("Loaded " + rows.size() + " trades.");
        return rows;
    }
}
