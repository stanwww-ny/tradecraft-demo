package io.tradecraft.oms.core.parentfx;

public sealed interface ParentFx
    permits WantParentCancel, CancelChildIfParentRequested,
            CancelAllActiveChildren, ParentCancelDone {
}

