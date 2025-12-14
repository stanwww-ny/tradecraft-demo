package io.tradecraft.oms.core.parentfx;

import io.tradecraft.common.id.ParentId;

public record ParentCancelDone(ParentId parentId, long tsNanos) implements ParentFx {}

