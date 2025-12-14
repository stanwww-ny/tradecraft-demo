package io.tradecraft.oms.core.parentfx;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ParentId;

public record CancelChildIfParentRequested(ParentId parentId, ChildId childId, long tsNanos) implements ParentFx {}

