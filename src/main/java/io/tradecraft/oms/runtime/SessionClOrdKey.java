package io.tradecraft.oms.runtime;

import io.tradecraft.common.id.ClOrdId;
import io.tradecraft.fixqfj.session.SessionKey;

public record SessionClOrdKey(
        SessionKey s, ClOrdId c
) {
}
