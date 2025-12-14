package io.tradecraft.fixqfj.session;

import org.junit.jupiter.api.Test;
import quickfix.SessionID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SessionIndexTest {

    @Test
    void registersAndLooksUp() {
        SessionIndex idx = new SessionIndex();

        // given
        SessionKey key = new SessionKey("FIX.4.4", "TRADER", "OMS", null);
        SessionID sid = new SessionID("FIX.4.4", "TRADER", "OMS", null);

        // initially nothing
        assertNull(idx.getSession(key));
        assertEquals(0, idx.sizeSessions());

        // when
        idx.putSession(key, sid);

        // then
        assertEquals(1, idx.sizeSessions());
        assertSame(sid, idx.getSession(key));
    }
}
