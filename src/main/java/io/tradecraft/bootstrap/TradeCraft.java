package io.tradecraft.bootstrap;

import java.util.concurrent.locks.LockSupport;

public final class TradeCraft {
    public static void main(String[] args) throws Exception {
        try (Bootstrap bootstrap = new Bootstrap(OmsFixAcceptorConfig.fromSystemProps())) {
            bootstrap.start();

            Runtime.getRuntime().addShutdownHook(
                    new Thread(bootstrap::stop, "tradecraft-shutdown")
            );

            LockSupport.park(); // blocks forever, no checked exception
        }
    }
}