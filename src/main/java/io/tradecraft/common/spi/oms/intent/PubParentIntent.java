package io.tradecraft.common.spi.oms.intent;

import io.tradecraft.common.spi.Outgoing;

/**
 * Intents published by OMS FSM to the SOR.
 */
public sealed interface PubParentIntent extends Outgoing
        permits ParentCancelIntent, ParentRouteIntent {
}
