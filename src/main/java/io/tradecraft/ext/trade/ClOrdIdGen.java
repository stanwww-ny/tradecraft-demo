package io.tradecraft.ext.trade;

import quickfix.SessionID;

public class ClOrdIdGen {
    public static String nextId(SessionID sessionID) {
        return sessionID.getSenderCompID() + "-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 10_000);
    }
}
