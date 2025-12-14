package io.tradecraft.oms.dispatch;

import io.tradecraft.common.envelope.Meta;
import io.tradecraft.oms.core.Effects;

public interface EffectPublisher {
    void publish(Effects effects, Meta meta);
}
