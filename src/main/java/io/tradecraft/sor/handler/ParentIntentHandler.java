package io.tradecraft.sor.handler;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;

public interface ParentIntentHandler {
    void onIntent(Envelope<PubParentIntent> msg);
}
