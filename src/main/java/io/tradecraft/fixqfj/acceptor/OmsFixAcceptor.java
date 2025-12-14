package io.tradecraft.fixqfj.acceptor;

import io.tradecraft.bootstrap.OmsFixAcceptorConfig;
import io.tradecraft.bootstrap.Lifecycle;
import quickfix.Acceptor;
import quickfix.ConfigError;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;

import java.util.Iterator;

public class OmsFixAcceptor implements Lifecycle {
    private final Acceptor acceptor;
    private final SessionID configuredSession; // NEW

    public OmsFixAcceptor(OmsFixAcceptorConfig omsFixAcceptorConfig,
                          OmsFixInbound omsFixInbound) throws ConfigError {
        this.acceptor = new SocketAcceptor(omsFixInbound, omsFixAcceptorConfig.storeFactory(), omsFixAcceptorConfig.sessionSettings(), omsFixAcceptorConfig.logFactory(), omsFixAcceptorConfig.messageFactory());
        this.configuredSession = readConfiguredSession(omsFixAcceptorConfig.sessionSettings());
    }

    @Override
    public void start() {
        try {
            acceptor.start();
        } catch (Exception e) {
            safeStop();
            throw new IllegalStateException("Failed to start QuickFIX/J acceptor", e);
        }
    }

    @Override
    public void stop() {
        safeStop();
    }

    private void safeStop() {
        try {
            var sessions = acceptor.getSessions();
            if (sessions != null) {
                for (SessionID sid : sessions) {
                    var s = quickfix.Session.lookupSession(sid);
                    if (s != null) {
                        try {
                            s.logout("OMS shutdown");
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
            acceptor.stop();
        } catch (RuntimeException ignored) {
            // Defensive shutdown: QuickFIX/J may throw on partial init
        }
    }

    private SessionID readConfiguredSession(SessionSettings settings) {
        try {
            Iterator<SessionID> it = settings.sectionIterator();
            return it.hasNext() ? it.next() : null;
        } catch (Exception t) {
            return null;
        }
    }

    public SessionID getDefaultSession() {
        var sessions = acceptor.getSessions();
        if (sessions != null && !sessions.isEmpty()) return sessions.get(0);
        return configuredSession; // fallback prevents null in tests
    }
}

