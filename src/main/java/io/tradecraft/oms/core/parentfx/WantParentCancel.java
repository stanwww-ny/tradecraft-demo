package io.tradecraft.oms.core.parentfx;

import io.tradecraft.common.id.ParentId;

public record WantParentCancel(ParentId parentId, long tsNanos) implements ParentFx {}

