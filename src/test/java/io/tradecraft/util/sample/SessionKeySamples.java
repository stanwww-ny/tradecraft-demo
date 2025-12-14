package io.tradecraft.util.sample;

import io.tradecraft.fixqfj.session.SessionKey;
import quickfix.SessionID;

public class SessionKeySamples {
    public static final SessionID SESSION_ID = new SessionID("FIX.4.4", "TRADER", "OMS");
    public static final SessionID SESSION_ID_Q = new SessionID("FIX.4.4", "TRADER", "OMS", "QUALIFIER");
    public static final SessionKey SESSION_KEY = SessionKey.of(SESSION_ID);
}
