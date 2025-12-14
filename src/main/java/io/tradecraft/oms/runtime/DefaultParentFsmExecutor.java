package io.tradecraft.oms.runtime;

import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.id.IntentId;
import io.tradecraft.common.id.allocator.IntentIdAllocator;
import io.tradecraft.common.spi.oms.exec.PubExecReport;
import io.tradecraft.common.spi.oms.intent.PubParentIntent;
import io.tradecraft.oms.core.Effects;
import io.tradecraft.oms.core.NewStateMapper;
import io.tradecraft.oms.core.OrderState;
import io.tradecraft.oms.core.ParentOrderFsm;
import io.tradecraft.oms.core.ParentStateStore;
import io.tradecraft.oms.core.parentfx.ParentFx;
import io.tradecraft.oms.event.EvNew;
import io.tradecraft.oms.event.OrderEvent;
import io.tradecraft.oms.repo.ParentFsmRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class DefaultParentFsmExecutor implements ParentFsmExecutor {

    private final ParentStateStore store;
    private final ParentFsmRepository fsmRepo;
    private final NewStateMapper newStateMapper;
    private final IntentIdAllocator intentIdAllocator;

    public DefaultParentFsmExecutor(
            ParentStateStore store,
            ParentFsmRepository fsmRepo,
            NewStateMapper newStateMapper,
            IntentIdAllocator intentIdAllocator
    ) {
        this.store = store;
        this.fsmRepo = fsmRepo;
        this.newStateMapper = newStateMapper;
        this.intentIdAllocator = intentIdAllocator;
    }

    @Override
    public Effects apply(OrderEvent event, Meta meta) {

        // ------------------------------------------------------------
        // 1) INITIALIZE NEW PARENT ORDER (EvNew)
        // ------------------------------------------------------------
        if (event instanceof EvNew evNew) {
            IntentId intentId = intentIdAllocator.allocate();
            OrderState newState = newStateMapper.from(evNew, intentId);
            store.put(newState);
        }

        // ------------------------------------------------------------
        // 2) PROCESS EVENT + FOLLOW-UPS
        // ------------------------------------------------------------
        return processEventChain(event);
    }

    private Effects processEventChain(OrderEvent initialEvent) {

        final int HOP_LIMIT = 8;

        ArrayDeque<OrderEvent> events = new ArrayDeque<>();
        events.add(initialEvent);

        List<PubExecReport> execReports = new ArrayList<>();
        List<PubParentIntent> intents = new ArrayList<>();
        List<ParentFx> parentFxes = new ArrayList<>();

        OrderState lastState = null;
        int hops = 0; // circuit breaker

        while (!events.isEmpty() && hops++ < HOP_LIMIT) {

            OrderEvent ev = events.removeFirst();

            ParentOrderFsm fsm = fsmRepo.get(ev.parentId());
            OrderState state = store.get(ev.parentId());

            Effects eff = fsm.apply(state, ev);

            // persist immediately
            store.put(eff.newState());
            lastState = eff.newState();

            // collect outputs explicitly
            execReports.addAll(eff.execReports());
            intents.addAll(eff.intents());
            parentFxes.addAll(eff.parentFxes());

            // enqueue follow-ups
            events.addAll(eff.followUps());
        }

        return new Effects.Builder(lastState)
                .ers(execReports).intents(intents)
                .parentFxes(parentFxes).build();
    }

}
