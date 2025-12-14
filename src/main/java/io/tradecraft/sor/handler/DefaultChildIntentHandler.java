package io.tradecraft.sor.handler;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.envelope.Stage;
import io.tradecraft.common.log.LogUtils;
import io.tradecraft.common.spi.sor.intent.CancelChildIntent;
import io.tradecraft.common.spi.sor.intent.NewChildIntent;
import io.tradecraft.common.spi.sor.intent.PubChildIntent;
import io.tradecraft.sor.core.ChildIntentReducer;
import io.tradecraft.sor.core.DefaultChildIntentReducer;
import io.tradecraft.sor.core.SorEffects;
import io.tradecraft.sor.policy.VenuePolicy;
import io.tradecraft.sor.routing.VenueRouter;
import io.tradecraft.sor.state.ChildState;
import io.tradecraft.sor.state.ChildStatus;
import io.tradecraft.sor.store.ChildStateStore;
import io.tradecraft.sor.store.ChildCtxStore;
import io.tradecraft.venue.cmd.VenueCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.tradecraft.common.meta.Component.SOR;
import static io.tradecraft.common.meta.Flow.IN;
import static io.tradecraft.common.meta.Flow.OUT;
import static io.tradecraft.common.meta.MessageType.CMD;

public final class DefaultChildIntentHandler implements ChildIntentHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultChildIntentHandler.class);

    private final VenueRouter venueRouter;
    private final ChildCtxStore childCtxStore;
    private final ChildStateStore childStateStore;
    private final ChildIntentReducer childIntentReducer;
    private final EnvelopeMetaFactory metaFactory;

    public DefaultChildIntentHandler(
            VenueRouter venueRouter,
            ChildCtxStore childCtxStore,
            ChildStateStore childStateStore,
            EnvelopeMetaFactory metaFactory) {
        this.venueRouter = Objects.requireNonNull(venueRouter, "router");
        this.childCtxStore = childCtxStore;
        this.childStateStore = childStateStore;
        VenuePolicy venuePolicy = null;
        this.childIntentReducer = new DefaultChildIntentReducer(venuePolicy);
        this.metaFactory = metaFactory;
    }

    private void safeSend(List<Envelope<VenueCommand>> cmds, String what) {
        LogUtils.log(SOR, CMD, OUT, this, cmds);
        for (Envelope<VenueCommand> envelope : cmds) {
            try {
                venueRouter.dispatch(envelope);
            } catch (Exception ex) {
                log.error("Failed to send {} to {}: {}", what, envelope.payload().venueId(), ex);
            }
        }
    }

    public void onIntent(Envelope<PubChildIntent> envelope) {
        PubChildIntent intent = envelope.payload();
        LogUtils.log(SOR, CMD, IN, this, intent);
        if (intent instanceof NewChildIntent) {
            onNewChild(envelope);
        } else if (intent instanceof CancelChildIntent) {
            onCancelChild(envelope);
        } else {
            log.warn("Unknown PubChildIntent: {}", intent);
        }
    }

    private void onNewChild(Envelope<PubChildIntent> envelope) {
        NewChildIntent i = (NewChildIntent) envelope.payload();
        Meta meta = envelope.meta();
        // --- 0) Idempotency guard ---
        ChildState cur = childStateStore.get(i.childId());
        if (cur != null) {
            if (cur.status().isTerminal()) return;              // ignore dup New for terminal child
            if (cur.childClOrdId() != null) return;             // already routed once → treat as replay
        }

        // 1) Choose venueId
        childCtxStore.put(i);

        childStateStore.upsert(i.childId(), () -> ChildState.builder(i.parentId(), i.childId(), i.tsNanos())
                .childClOrdId(i.childClOrdId())
                .instrumentKey(i.instrumentKey())
                .side(i.side())
                .ordType(i.ordType())
                .qty(i.qty())
                .lastPxMicros(i.priceMicros())
                .venueId(i.venueId())
                .status(ChildStatus.NEW_PENDING)
                .updatedTsNanos(i.tsNanos())
                .build());

        SorEffects eff = childStateStore.apply(
                i.childId(), i,
                (state, intent) -> childIntentReducer.reduce(state, i)
        );
        this.metaFactory.addHop(meta, Stage.SOR_SEND_CHILD_ORDER);
        List<Envelope<VenueCommand>> envelopes = getEnvelopes(eff, meta);
        safeSend(envelopes, "NewChildCmd");
        // Optionally publish PENDING_NEW here back to OMS
    }

    private static List<Envelope<VenueCommand>> getEnvelopes(SorEffects eff, Meta meta) {
        List<Envelope<VenueCommand>> envelopes = new ArrayList<>();
        if (!eff.venueCommands().isEmpty()) {
            for (VenueCommand venueCommand : eff.venueCommands()) {
                envelopes.add(Envelope.of(venueCommand, meta));
            }
        }
        return envelopes;
    }

    private void onCancelChild(Envelope<PubChildIntent> envelope) {
        CancelChildIntent c = (CancelChildIntent) envelope.payload();
        Meta meta = envelope.meta();
        // Persist PENDING_CANCEL before emitting the venue cancel, to handle races
        if (c.childId() != null) {
            SorEffects eff = childStateStore.apply(
                    c.childId(),
                    c,
                    (state, intent) -> childIntentReducer.reduce(state, c)
            );
            this.metaFactory.addHop(meta, Stage.SOR_SEND_CANCEL_ORDER);
            List<Envelope<VenueCommand>> envelopes = getEnvelopes(eff, meta);
            safeSend(envelopes, "ChildCancelCmd");
        }
    }

}
