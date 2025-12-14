package io.tradecraft.oms.runtime;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.spi.oms.intent.ParentCancelIntent;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.oms.core.OrderState;
import io.tradecraft.oms.core.parentfx.CancelAllActiveChildren;
import io.tradecraft.oms.core.parentfx.CancelChildIfParentRequested;
import io.tradecraft.oms.core.parentfx.ParentCancelDone;
import io.tradecraft.oms.core.parentfx.ParentFx;
import io.tradecraft.oms.core.parentfx.WantParentCancel;
import io.tradecraft.oms.event.EventQueue;

import java.util.List;

public final class DefaultParentFxProcessor implements ParentFxProcessor {

    private final ParentCancelRegistry cancelRegistry;
    private final EventQueue<Envelope<PubParentIntent>> intentBus;

    public DefaultParentFxProcessor(ParentCancelRegistry cancelRegistry,
                                    EventQueue<Envelope<PubParentIntent>> intentBus) {
        this.cancelRegistry = cancelRegistry;
        this.intentBus = intentBus;
    }

    @Override
    public void processFx(List<ParentFx> fxList, OrderState st) {
        for (ParentFx fx : fxList) {

            if (fx instanceof WantParentCancel w) {
                handleWantParentCancel(w);

            } else if (fx instanceof CancelAllActiveChildren c) {
                handleCancelAllChildren(st);

            } else if (fx instanceof CancelChildIfParentRequested ccr) {
                handleConditionalChildCancel(ccr, st);

            } else if (fx instanceof ParentCancelDone d) {
                handleParentCancelDone(d);
            }
        }
    }

    private void handleWantParentCancel(WantParentCancel fx) {
        cancelRegistry.mark(fx.parentId());
    }

    private void handleCancelAllChildren(OrderState st) {
        for (ChildId cid : st.children().keySet()) {
            if (cancelRegistry.markChildIfFirst(st.parentId(), cid)) {
                ParentCancelIntent intent = ParentCancelIntent.builder()
                        .parentId(st.parentId())
                        .childId(cid)
                        .instrumentKey(st.instrumentKey())
                        .side(st.side())
                        .tsNanos(st.lastTsNanos())
                        .build();

                intentBus.offer(Envelope.of(intent));
            }
        }
    }

    private void handleConditionalChildCancel(CancelChildIfParentRequested fx, OrderState st) {
        var pid = fx.parentId();
        var cid = fx.childId();

        if (cancelRegistry.isMarked(pid) && cancelRegistry.markChildIfFirst(pid, cid)) {
            ParentCancelIntent intent = ParentCancelIntent.builder()
                    .parentId(pid)
                    .childId(cid)
                    .instrumentKey(st.instrumentKey())
                    .side(st.side())
                    .tsNanos(st.lastTsNanos())
                    .build();

            intentBus.offer(Envelope.of(intent));
        }
    }

    private void handleParentCancelDone(ParentCancelDone fx) {
        cancelRegistry.clear(fx.parentId());
    }
}
