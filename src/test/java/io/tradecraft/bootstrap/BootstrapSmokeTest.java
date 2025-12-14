// src/test/java/org/example/v4/AppSmokeTest.java
package io.tradecraft.bootstrap;

import io.tradecraft.oms.testutil.TestUtils;
import org.junit.jupiter.api.Test;
import quickfix.DefaultMessageFactory;
import quickfix.MemoryStoreFactory;
import quickfix.SLF4JLogFactory;
import quickfix.SessionSettings;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapSmokeTest {

    private static final String MINI_ACCEPTOR_CFG = """
            [default]
            ConnectionType=acceptor
            ReconnectInterval=1
            StartTime=00:00:00
            EndTime=23:59:59
            UseDataDictionary=N
            FileStorePath=target/qfj-store
            FileLogPath=target/qfj-log
            SocketAcceptPort=9882
            SocketReuseAddress=Y
            HeartBtInt=30
            
            [session]
            BeginString=FIX.4.4
            SenderCompID=OMS
            TargetCompID=CLIENT
            """;

    @Test
    void app_starts_and_stops_cleanly() throws Exception {
        SessionSettings settings = TestUtils.qfjSettingsFrom(MINI_ACCEPTOR_CFG);
        var cfg = new OmsFixAcceptorConfig(
                settings,
                new MemoryStoreFactory(),
                new SLF4JLogFactory(settings),
                new DefaultMessageFactory(),
                "Pipeline-IT-0"
        );
        try (var bootstrap = new Bootstrap(cfg)) {
            assertDoesNotThrow(bootstrap::start);
            TestUtils.sleepQuietly(java.time.Duration.ofMillis(300));
            assertTrue(TestUtils.anyThreadWithPrefix("Pipeline-IT"), "pipeline thread should be running");
        }
    }
}
