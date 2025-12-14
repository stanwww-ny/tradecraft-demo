package io.tradecraft.bootstrap;

import io.tradecraft.oms.support.ThreadNames;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.LogFactory;
import quickfix.MemoryStoreFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SLF4JLogFactory;
import quickfix.SessionSettings;

public record OmsFixAcceptorConfig(
        SessionSettings sessionSettings,
        MessageStoreFactory storeFactory,
        LogFactory logFactory,
        MessageFactory messageFactory,
        String pipelineThreadName
) {
    public static OmsFixAcceptorConfig fromSystemProps() throws ConfigError {
        var settings = new SessionSettings(System.getProperty("fix.settings", "quickfix/acceptor.cfg"));
        return new OmsFixAcceptorConfig(
                settings,
                new MemoryStoreFactory(),
                new SLF4JLogFactory(settings),
                new DefaultMessageFactory(),
                ThreadNames.pipeline(0)
        );
    }
}
