package io.tradecraft.fixqfj.session;

import io.tradecraft.common.id.ParentId;

public interface ParentSessionBinder {
    void bindParent(ParentId parentId, SessionKey outboundKey);
}
