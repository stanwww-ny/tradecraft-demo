package io.tradecraft.fixqfj.acceptor;

import io.tradecraft.common.domain.time.DualTimeSource;
import quickfix.Application;
import quickfix.ConfigError;
import quickfix.Connector;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SLF4JLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.SocketInitiator;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility to build and manage QuickFIX/J Acceptor or Initiator with a clean API.
 */
public final class FixRuntimeBuilder {
    /**
     * Choose a MessageFactory either by explicit FixVersion or by BeginString in settings.
     */
    private static MessageFactory createMessageFactory(SessionSettings settings, FixVersion v) {
        if (v != FixVersion.AUTO) return factoryFor(v);

        // AUTO: attempt to detect from any session's BeginString
        Iterator<SessionID> it = settings.sectionIterator();
        while (it.hasNext()) {
            SessionID id = it.next();
            try {
                String begin = settings.getString(id, SessionSettings.BEGINSTRING);
                if ("FIX.4.4".equals(begin)) return new quickfix.fix44.MessageFactory();
                if ("FIXT.1.1".equals(begin)) {
                    // Often paired with ApplVerID=FIX50SP2
                    try {
                        String applVer = settings.getString(id, "DefaultApplVerID");
                        if ("9".equals(applVer) || "FIX50SP2".equalsIgnoreCase(applVer)) {
                            return new quickfix.fix50sp2.MessageFactory();
                        }
                    } catch (ConfigError ignored) {
                    }
                    // Fallback to FIX44 if uncertain
                    return new quickfix.fix44.MessageFactory();
                }
            } catch (ConfigError ignored) {
            }
        }
        // Default (common case): FIX44
        return new quickfix.fix44.MessageFactory();
    }

    private static MessageFactory factoryFor(FixVersion v) {
        return switch (v) {
            case FIX44 -> new quickfix.fix44.MessageFactory();
            case FIX50SP2 -> new quickfix.fix50sp2.MessageFactory();
            case AUTO -> new quickfix.fix44.MessageFactory(); // unreachable due to guard above
        };
    }

    private void FixRuntime() {
    }

    public enum Mode {ACCEPTOR, INITIATOR}

    // ---- internals ----

    public enum FixVersion {FIX44, FIX50SP2, AUTO}

    public static final class Builder {
        private Mode mode = Mode.ACCEPTOR;
        private Application app;
        private SessionSettings settings;
        private String cfgFile;
        private InputStream cfgStream;
        private FixVersion fixVersion = FixVersion.AUTO;
        private boolean addShutdownHook = true;
        private DualTimeSource dualTimeSource;

        private static SessionSettings loadSettings(String cfgLocation)
                throws ConfigError, IOException {

            String path = cfgLocation;
            if (path.startsWith("classpath:")) {
                path = path.substring("classpath:".length());
            }

            // 1) Try classpath (both with and without "classpath:" prefix)
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            InputStream is = cl.getResourceAsStream(path);
            if (is == null && !cfgLocation.startsWith("classpath:")) {
                is = cl.getResourceAsStream(cfgLocation);
            }

            if (is != null) {
                try (InputStream in = is) {
                    return new SessionSettings(in); // reads fully; safe to close
                }
            }

            // 2) Fall back to filesystem path
            return new SessionSettings(cfgLocation);
        }

        public Builder mode(Mode mode) {
            this.mode = Objects.requireNonNull(mode);
            return this;
        }

        public Builder application(Application app) {
            this.app = Objects.requireNonNull(app);
            return this;
        }

        /**
         * Load settings from file path.
         */
        public Builder settings(String cfgFile) {
            this.cfgFile = Objects.requireNonNull(cfgFile);
            return this;
        }

        /**
         * Load settings from an existing InputStream (caller's responsibility to close).
         */
        public Builder settings(InputStream cfgStream) {
            this.cfgStream = Objects.requireNonNull(cfgStream);
            return this;
        }

        /**
         * Force a message factory version; otherwise AUTO detects from settings BeginString.
         */
        public Builder fixVersion(FixVersion v) {
            this.fixVersion = Objects.requireNonNull(v);
            return this;
        }

        /**
         * Register a JVM shutdown hook that calls stop() when the process quits.
         */
        public Builder withShutdownHook(boolean enable) {
            this.addShutdownHook = enable;
            return this;
        }

        public Builder dualTimeSource(DualTimeSource dualTimeSource) {
            this.dualTimeSource = dualTimeSource;
            return this;
        }

        public FixRuntime build() throws Exception {
            if (app == null) throw new IllegalStateException("Application is required");
            if (cfgFile == null && cfgStream == null) throw new IllegalStateException("Settings source is required");

            if (cfgStream != null) {
                settings = new SessionSettings(cfgStream);
            } else {
                settings = loadSettings(cfgFile);
            }

            // Factories
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            LogFactory logFactory = new SLF4JLogFactory(settings);
            MessageFactory messageFactory = createMessageFactory(settings, fixVersion);

            // Acceptor or Initiator
            Connector connector = (mode == Mode.ACCEPTOR)
                    ? new SocketAcceptor(app, storeFactory, settings, logFactory, messageFactory)
                    : new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);

            return new FixRuntime(connector, settings, addShutdownHook, dualTimeSource);
        }
    }

    /**
     * Wrapper providing lifecycle, utilities, and simple health.
     */
    public static final class FixRuntime {
        private final Connector connector;
        private final SessionSettings settings;
        private final CountDownLatch keepAlive = new CountDownLatch(1);
        private final DualTimeSource dualTimeSource;
        private Thread shutdownHook;
        private volatile boolean started = false;

        private FixRuntime(Connector connector, SessionSettings settings, boolean withHook, DualTimeSource dualTimeSource) {
            this.connector = connector;
            this.settings = settings;
            if (withHook) {
                shutdownHook = new Thread(this::stop, "fixhelper-shutdown");
                Runtime.getRuntime().addShutdownHook(shutdownHook);
            }
            this.dualTimeSource = dualTimeSource;
        }

        /**
         * Starts the connector. Idempotent-ish.
         */
        public synchronized void start() throws ConfigError {
            if (started) return;
            connector.start();
            started = true;
        }

        /**
         * Stops the connector and releases await(). Safe to call multiple times.
         */
        public synchronized void stop() {
            if (!started) return;
            try {
                connector.stop(true);
            } catch (Exception ignore) {
            }
            started = false;
            keepAlive.countDown();
            if (shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (IllegalStateException ignore) {
                }
                shutdownHook = null;
            }
        }

        /**
         * Blocks until stop() is called (intentional keep-alive).
         */
        public void await() throws InterruptedException {
            keepAlive.await();
        }

        /**
         * Blocks with timeout; returns false on timeout.
         */
        public boolean await(Duration timeout) throws InterruptedException {
            return keepAlive.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public boolean isRunning() {
            return started;
        }

        /**
         * Returns all configured sessions.
         */
        public Collection<SessionID> sessions() {
            Set<SessionID> out = new LinkedHashSet<>();
            for (Iterator<SessionID> it = connector.getSessions().iterator(); it.hasNext(); ) out.add(it.next());
            return Collections.unmodifiableSet(out);
        }

        /**
         * Wait until all sessions are logged on or timeout.
         */
        public boolean awaitLogon(Duration timeout) throws InterruptedException {
            long deadline = dualTimeSource.nowNanos() + timeout.toNanos();
            for (SessionID id : sessions()) {
                while (!Session.lookupSession(id).isLoggedOn()) {
                    long remaining = deadline - dualTimeSource.nowNanos();
                    if (remaining <= 0) return false;
                    Thread.sleep(Math.min(TimeUnit.NANOSECONDS.toMillis(remaining), 200));
                }
            }
            return true;
        }

        public boolean isLoggedOn(SessionID id) {
            Session s = Session.lookupSession(id);
            return s != null && s.isLoggedOn();
        }

        /**
         * Safe send helper: returns true if sent, false otherwise (no throw).
         */
        public boolean sendToTargetSafe(Message msg, SessionID id) {
            try {
                return Session.sendToTarget(msg, id);
            } catch (SessionNotFound e) {
                System.err.println("[FIX] Session not found: " + id + " - " + e.getMessage());
                return false;
            }
        }

        /**
         * Convenience: reset sequence numbers for a session (e.g., before logon).
         */
        public void resetSequenceNumbers(SessionID id) {
            Session s = Session.lookupSession(id);
            if (s != null) {
                try {
                    s.reset();
                } catch (Exception e) {
                    System.err.println("[FIX] Reset failed for " + id + ": " + e.getMessage());
                }
            }
        }

        public SessionSettings settings() {
            return settings;
        }
    }
}

