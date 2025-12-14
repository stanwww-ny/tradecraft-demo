package io.tradecraft.fixqfj.session;

import io.tradecraft.common.spi.oms.exec.PubExecReport;
import quickfix.SessionID;

@FunctionalInterface
public interface SessionResolver {
    SessionID resolve(PubExecReport er);
}
