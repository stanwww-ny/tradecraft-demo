package io.tradecraft.oms.runtime;

import io.tradecraft.oms.core.OrderState;
import io.tradecraft.oms.core.parentfx.ParentFx;

import java.util.List;

public interface ParentFxProcessor {
    void processFx(List<ParentFx> fxList, OrderState state);
}
