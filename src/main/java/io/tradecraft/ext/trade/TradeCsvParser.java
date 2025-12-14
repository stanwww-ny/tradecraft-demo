package io.tradecraft.ext.trade;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses trade instructions from a CSV file.
 */
public final class TradeCsvParser implements TradeParser {

    public TradeCsvParser() {
    }

    /**
     * Load CSV from filesystem path or, if missing, from classpath resources.
     */
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

            String account = c.length >= 1 ? c[0] : "";
            String symbol = c.length >= 2 ? c[1] : "";
            String side = c.length >= 3 ? c[2] : "";
            long qty = c.length >= 4 && !c[3].isBlank() ? Long.parseLong(c[3]) : 0L;
            String ordType = c.length >= 5 ? c[4] : "";
            String px = c.length >= 6 ? c[5] : "";
            String tif = c.length >= 7 ? c[6] : "";
            String exDest = c.length >= 8 ? c[7] : "";
            String handl = c.length >= 9 ? c[8] : "";

            Double limit = (px == null || px.isBlank()) ? null : Double.parseDouble(px);

            if (account.isEmpty() || symbol.isEmpty() || side.isEmpty() || ordType.isEmpty() || tif.isEmpty()) {
                System.err.println("Skipping row " + rowNum + " (missing required fields): " + line);
                continue;
            }
            rows.add(new Trade(account, symbol, side, qty, ordType, limit, tif, exDest, handl));
        }
        System.out.println("Loaded " + rows.size() + " trades.");
        return rows;
    }
}
