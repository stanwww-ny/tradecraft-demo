package io.tradecraft.oms.runtime;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.oms.core.parentfx.ParentFx;
import io.tradecraft.oms.event.OrderEvent;

import java.util.List;

public interface EventTranslator {

    /** Convert raw inbound OMS/SOR event → OMS internal event */
    OrderEvent translate(Envelope<OrderEvent> env, Meta meta);

    /** Generate ParentFx that must run BEFORE FSM (e.g., EvChildAck → CancelChildIfParentRequested) */
    List<ParentFx> preFsmFx(Envelope<OrderEvent> env);
}

