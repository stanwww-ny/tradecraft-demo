package io.tradecraft.fixqfj.event;

import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.fixqfj.session.SessionKey;

public sealed interface FixEvInbound permits FixEvParentNew, FixEvCancelReq, FixEvReplaceReq {
    SessionKey sessionKey();

    ClOrdId clOrdId();

    long ingressNanos();
}