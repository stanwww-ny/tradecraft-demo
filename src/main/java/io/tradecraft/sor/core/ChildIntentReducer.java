package io.tradecraft.sor.core;

import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.sor.state.ChildState;

public interface ChildIntentReducer {
    SorEffects reduce(ChildState state, PubChildIntent ev);
}
