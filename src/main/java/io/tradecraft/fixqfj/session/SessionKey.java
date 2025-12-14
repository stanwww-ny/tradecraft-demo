package io.tradecraft.fixqfj.session;

import quickfix.SessionID;

/**
 * Unique key for a FIX session. Example: FIX.4.4:OMS->TRADER[:SIM]
 */
public record SessionKey(String beginString, String senderCompID, String targetCompID, String sessionQualifier) {

    /**
     * Build from a QFJ SessionID (qualifier may be null/empty).
     */
    public static SessionKey of(SessionID sid) {
        return new SessionKey(
                sid.getBeginString(),
                sid.getSenderCompID(),
                sid.getTargetCompID(),
                sid.getSessionQualifier() // may be null
        );
    }

    public static SessionKey of(String beginString, String senderCompID, String targetCompID, String sessionQualifier) {
        return new SessionKey(
                beginString,
                senderCompID,
                targetCompID,
                sessionQualifier// may be null
        );
    }

    /**
     * Swap sender/target; keep qualifier. TRADER->OMS => OMS->TRADER
     */
    public SessionKey reverse() {
        return new SessionKey(beginString, targetCompID, senderCompID, sessionQualifier);
    }

    /**
     * Human-readable form; appends :QUAL only if non-empty.
     */
    @Override
    public String toString() {
        if (sessionQualifier == null || sessionQualifier.isEmpty()) {
            return beginString + ":" + senderCompID + "->" + targetCompID;
        }
        return beginString + ":" + senderCompID + "->" + targetCompID + ":" + sessionQualifier;
    }
}
