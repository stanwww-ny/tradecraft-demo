package io.tradecraft.sor.handler;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;

public interface ChildIntentHandler {
    void onIntent(Envelope<PubChildIntent> msg);
}
