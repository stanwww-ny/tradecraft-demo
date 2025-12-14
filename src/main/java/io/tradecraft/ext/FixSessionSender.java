package io.tradecraft.ext;

import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.fix44.Message;

/**
 * Wraps Session.sendToTarget with a tiny helper.
 */
public final class FixSessionSender {
    private FixSessionSender() {
    }

    public static void send(SessionID sessionID, Message msg) {
        try {
            Session.sendToTarget(msg, sessionID);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }
}
