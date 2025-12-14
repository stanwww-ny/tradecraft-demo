package io.tradecraft.common.id;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.Enumeration;

/**
 * Utilities to derive stable (per-node) and high-entropy (per-boot) seeds.
 */
public final class StableIds {
    private StableIds() {
    }

    /**
     * Fresh, high-entropy 64-bit seed per process boot. Use in production.
     */
    public static long bootSeedSecure() {
        return new SecureRandom().nextLong();
    }

    /**
     * Stable per-node salt to make ID streams disjoint across machines/pods. Prefers explicit env var; falls back to
     * hostname+MAC; then user+pid.
     */
    public static long nodeSalt() {
        // 1) Explicit node id if provided (best for clusters)
        String explicit = System.getenv("TRADECRAFT_NODE_ID");
        if (explicit != null && !explicit.isBlank()) {
            try {
                return Long.parseLong(explicit);
            } catch (NumberFormatException ignore) {/* fall through */}
            return mix64(explicit.getBytes());
        }

        try {
            // 2) Hostname + first non-null MAC (works on macOS/Linux/containers)
            String host = InetAddress.getLocalHost().getHostName();
            String mac = firstMacHex();
            return mix64((host + "|" + mac).getBytes());
        } catch (Exception e) {
            // 3) Fallback for restricted sandboxes/CI
            String fallback = System.getProperty("user.name", "unknown")
                    + "-" + ProcessHandle.current().pid();
            return mix64(fallback.getBytes());
        }
    }

    private static String firstMacHex() throws Exception {
        Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
        while (nics.hasMoreElements()) {
            NetworkInterface ni = nics.nextElement();
            byte[] mac = ni.getHardwareAddress();
            if (mac != null && mac.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (byte b : mac) sb.append(String.format("%02x", b));
                return sb.toString();
            }
        }
        return "";
    }

    /**
     * Small, fast 64-bit mixer (golden-ratio seeded).
     */
    private static long mix64(byte[] bytes) {
        long h = 0x9e3779b97f4a7c15L;
        for (byte b : bytes) {
            h ^= (b & 0xff);
            h *= 0xff51afd7ed558ccdL;
            h ^= (h >>> 33);
        }
        return h;
    }
}
