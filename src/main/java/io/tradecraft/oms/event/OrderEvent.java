package io.tradecraft.oms.event;

import io.tradecraft.common.id.ParentId;

public sealed interface OrderEvent
        permits EvAck, EvBoundCancelReq, EvBoundParentNew, EvBoundReplaceReq, EvCancelAck, EvCancelReq, EvChildAck, EvChildCancelReject, EvChildCanceled, EvChildFill, EvChildPendingCancel, EvChildReject, EvChildReplaceReject, EvChildReplaced, EvError, EvFill, EvNew, EvParentAck, EvReject, EvReplaceReq {
    ParentId parentId();
    long tsNanos();
}


