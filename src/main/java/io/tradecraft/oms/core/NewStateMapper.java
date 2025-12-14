package io.tradecraft.oms.core;

import io.tradecraft.common.id.IntentId;
import io.tradecraft.oms.event.EvNew;

public interface NewStateMapper {
    OrderState from(EvNew e, IntentId intentId);
}

