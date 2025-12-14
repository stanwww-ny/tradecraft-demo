package io.tradecraft.fixqfj.session;

import io.tradecraft.common.id.ParentId;
import quickfix.SessionID;

import java.util.concurrent.ConcurrentHashMap;

public final class SessionIndex implements ParentSessionBinder{
    private final ConcurrentHashMap<SessionKey, SessionID> byKey = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ParentId, SessionKey> parentToKey = new ConcurrentHashMap<>();

    public void putSession(SessionKey key, SessionID sid) {
        if (key != null && sid != null && !byKey.containsKey(key)) byKey.put(key, sid);
    }

    public SessionID getSession(SessionKey key) {
        return (key == null) ? null : byKey.get(key);
    }

    /**
     * Bind once when ParentOrderId is created in Pipeline (using inboundKey.reverse()).
     */
    public void bindParent(ParentId parentId, SessionKey outboundKey) {
        if (parentId != null && outboundKey != null) parentToKey.put(parentId, outboundKey);
    }

    public SessionID getByParent(ParentId parentId) {
        SessionKey sessionKey = getKeyByParent(parentId);
        return (sessionKey == null) ? null : byKey.get(sessionKey);
    }

    public SessionKey getKeyByParent(ParentId parentId) {
        return (parentId == null) ? null : parentToKey.get(parentId);
    }

    /**
     * Optional: cleanup on logout.
     */
    public void removeAllFor(SessionID sid) {
        if (sid == null) return;
        // remove the SessionKey -> SessionID entry
        byKey.entrySet().removeIf(e -> e.getValue().equals(sid));
        // remove all parent bindings pointing to any removed keys
        parentToKey.entrySet().removeIf(e -> !byKey.containsKey(e.getValue()));
    }

    public int sizeSessions() {
        return byKey.size();
    }

    public int sizeParents() {
        return parentToKey.size();
    }
}
