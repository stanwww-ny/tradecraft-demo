package io.tradecraft.fixqfj.acceptor;

import io.tradecraft.bootstrap.OmsFixAcceptorConfig;
import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.fixqfj.session.SessionIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import quickfix.DefaultMessageFactory;
import quickfix.LogFactory;
import quickfix.MemoryStoreFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OmsFixAcceptorTest {

    OmsFixAcceptorConfig omsFixAcceptorConfig;
    OmsFixInbound omsFixInbound;
    SessionIndex sessionIndex;
    DualTimeSource dualTimeSource;

    @BeforeEach
    void setUp() {
        omsFixAcceptorConfig = mock(OmsFixAcceptorConfig.class);
        SessionSettings settings = inMemorySettings();
        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        LogFactory logFactory = new ScreenLogFactory(false, false, false);
        MessageFactory messageFactory = new DefaultMessageFactory();
        when(omsFixAcceptorConfig.sessionSettings()).thenReturn(settings);
        when(omsFixAcceptorConfig.logFactory()).thenReturn(logFactory);
        when(omsFixAcceptorConfig.messageFactory()).thenReturn(messageFactory);
        when(omsFixAcceptorConfig.storeFactory()).thenReturn(storeFactory);

        omsFixInbound = mock(OmsFixInbound.class);
        sessionIndex = new SessionIndex();
        dualTimeSource = mock(DualTimeSource.class);
    }

    private static SessionSettings inMemorySettings() {
        // Minimal acceptor + one FIX.4.4 session. We won't call start(), so the port won’t be used.
        try {
            String txt = """
                    [default]
                    ConnectionType=acceptor
                    StartTime=00:00:00
                    EndTime=23:59:59
                    UseDataDictionary=N
                    SocketAcceptPort=9880
                    SocketReuseAddress=Y
                    HeartBtInt=30
                    FileStorePath=target/fix/store
                    
                    [session]
                    BeginString=FIX.4.4
                    SenderCompID=OMS
                    TargetCompID=TRADER
                    #SessionQualifier=DEFAULT
                    """;
            return new SessionSettings(new ByteArrayInputStream(txt.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;

    }

    @Test
    @DisplayName("getDefaultSession returns the first configured SessionID (without starting the acceptor)")
    void getReadConfiguredSession_returnsFirstConfiguredSession() throws Exception {
        // GIVEN: in-memory settings + in-memory factories
        // Light test doubles for constructor deps
        @SuppressWarnings("unchecked")

        // WHEN: constructing the server (no networking done yet)
        OmsFixAcceptor server = new OmsFixAcceptor(omsFixAcceptorConfig, omsFixInbound);

        // THEN: a non-null default session should be visible from the configured settings
        SessionID sid = server.getDefaultSession();
        assertNotNull(sid, "SessionID should not be null");
        assertEquals("FIX.4.4", sid.getBeginString());
        assertEquals("OMS", sid.getSenderCompID());
        assertEquals("TRADER", sid.getTargetCompID());

        // Safety: do not start(); just ensure stop() is a no-op pre-start.
        server.stop();
    }

    @Test
    @DisplayName("start() initializes sessions; getDefaultSession() becomes non-null")
    void getReadConfiguredSession_afterStart_isNonNull() throws Exception {
        // FIX settings + factories
        OmsFixAcceptor server = new OmsFixAcceptor(omsFixAcceptorConfig, omsFixInbound);

        try {
            // Critical: start the acceptor so QuickFIX/J creates Session(s)
            server.start();
            SessionID sid = server.getDefaultSession();
            assertNotNull(sid, "SessionID should not be null after start()");
            assertEquals("FIX.4.4", sid.getBeginString());
            assertEquals("OMS", sid.getSenderCompID());
            assertEquals("TRADER", sid.getTargetCompID());
        } finally {
            server.stop();
        }
    }
}

