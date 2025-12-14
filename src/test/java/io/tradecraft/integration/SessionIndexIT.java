package io.tradecraft.integration;

import io.tradecraft.common.id.ParentId;
import io.tradecraft.fixqfj.session.SessionIndex;
import io.tradecraft.fixqfj.session.SessionKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.SessionID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that OmsFixServerModule and ErDrainerModule can share SessionIndex: - OMS registers sessions (putSession) -
 * Pipeline binds ParentOrderId -> SessionKey (bindParent) - Drainer looks up by Parent or by SessionKey (getByParent /
 * getSession) - Cleanup via removeAllFor
 */
class SessionIndexIT {

    private SessionIndex sessionIndex;

    // Known fixtures
    private SessionKey inboundKey;     // e.g., inbound TRADER->OMS
    private SessionKey outboundKey;    // e.g., outbound OMS->TRADER (could be "reverse" of inbound)
    private SessionID sid;
    private ParentId parent1;

    @BeforeEach
    void setUp() {
        sessionIndex = new SessionIndex();

        // Example keys — adjust ctor to your actual SessionKey signature
        inboundKey = new SessionKey("FIX.4.4", "TRADER", "OMS", null);
        outboundKey = new SessionKey("FIX.4.4", "OMS", "TRADER", null); // typical reverse

        sid = new SessionID("FIX.4.4", "TRADER", "OMS", null);
        parent1 = new ParentId("P-10001");
    }

    @Test
    void omsRegistersSession_andDrainerCanLookupByKey() {
        // OMS side: register the inbound session
        sessionIndex.putSession(inboundKey, sid);

        assertEquals(1, sessionIndex.sizeSessions());
        assertSame(sid, sessionIndex.getSession(inboundKey));

        // Drainer side: uses the same index -> can read it by key
        SessionID resolved = sessionIndex.getSession(inboundKey);
        assertNotNull(resolved);
        assertEquals(sid, resolved);
    }

    @Test
    void pipelineBindsParent_toOutboundKey_andDrainerLooksUpByParent() {
        // Given: inbound session registered
        sessionIndex.putSession(outboundKey, sid);

        // Pipeline binds parent to *outbound* key (what you said you do after inboundKey.reverse())
        sessionIndex.bindParent(parent1, outboundKey);

        assertEquals(1, sessionIndex.sizeParents());
        assertEquals(outboundKey, sessionIndex.getKeyByParent(parent1));

        // Drainer (or any publisher) can now route by ParentOrderId
        SessionID viaParent = sessionIndex.getByParent(parent1);
        assertNotNull(viaParent);
        assertEquals(sid, viaParent);
    }

    @Test
    void removeAllFor_removesBoth_session_and_all_parent_bindings_to_removed_keys() {
        // Seed: two sessions + two parents
        SessionKey keyA = inboundKey;
        SessionKey keyB = new SessionKey("FIX.4.4", "OMS2", "TRADER2", null);
        SessionID sidA = sid;
        SessionID sidB = new SessionID("FIX.4.4", "OMS2", "TRADER2", null);
        ParentId pA = parent1;
        ParentId pB = new ParentId("P-20002");

        sessionIndex.putSession(keyA, sidA);
        sessionIndex.putSession(keyB, sidB);
        sessionIndex.bindParent(pA, keyA);
        sessionIndex.bindParent(pB, keyB);

        assertEquals(2, sessionIndex.sizeSessions());
        assertEquals(2, sessionIndex.sizeParents());

        // Act: remove all entries for sidA
        sessionIndex.removeAllFor(sidA);

        // Assert: sidA and everything bound to its key are gone
        assertNull(sessionIndex.getSession(keyA));
        assertNull(sessionIndex.getByParent(pA));
        // sidB remains
        assertSame(sidB, sessionIndex.getSession(keyB));
        assertSame(sidB, sessionIndex.getByParent(pB));

        // size assertions (order of removals is implementation-dependent, but should reflect 1 left)
        assertEquals(1, sessionIndex.sizeSessions());
        assertEquals(1, sessionIndex.sizeParents());
    }

    @Test
    void putSession_is_idempotent_per_key_and_null_safe() {
        // null safety
        sessionIndex.putSession(null, sid);
        sessionIndex.putSession(inboundKey, null);
        assertEquals(0, sessionIndex.sizeSessions());

        // idempotency on same key
        sessionIndex.putSession(inboundKey, sid);
        sessionIndex.putSession(inboundKey, sid);
        assertEquals(1, sessionIndex.sizeSessions());
        assertSame(sid, sessionIndex.getSession(inboundKey));
    }
}
