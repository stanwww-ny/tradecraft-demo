package io.tradecraft.sor.handler;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.common.envelope.Meta;
import io.tradecraft.common.envelope.Stage;
import io.tradecraft.common.log.LogUtils;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.oms.event.OrderEvent;
import io.tradecraft.sor.core.ChildVenueReducer;
import io.tradecraft.sor.core.DefaultChildVenueReducer;
import io.tradecraft.sor.core.SorEffects;
import io.tradecraft.sor.store.ChildStateStore;
import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueCancelDone;
import io.tradecraft.venue.event.VenueEvent;
import io.tradecraft.venue.event.VenueFill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.tradecraft.common.meta.Flow.IN;
import static io.tradecraft.common.meta.MessageType.EV;
import static io.tradecraft.common.meta.Component.SOR;

public final class DefaultVenueHandler implements VenueHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultVenueHandler.class);
    private final ChildStateStore stateStore;
    private final EventQueue<Envelope<OrderEvent>> sorEvtBus;
    private final ChildVenueReducer childVenueReducer;
    private final EnvelopeMetaFactory metaFactory;

    public DefaultVenueHandler(EventQueue<Envelope<OrderEvent>> sorEvtBus, ChildStateStore childStateStore, EnvelopeMetaFactory metaFactory) {
        this.sorEvtBus = sorEvtBus;
        this.stateStore = childStateStore;
        this.childVenueReducer = new DefaultChildVenueReducer();
        this.metaFactory = metaFactory;
    }

    @Override
    public void onVenue(Envelope<VenueEvent> envelope) {
        VenueEvent e = envelope.payload();
        LogUtils.log(SOR, EV, IN, this, e);
        if (e instanceof VenueAck a) {
            handleAck(envelope);
        } else if (e instanceof VenueFill f) {
            handleFill(envelope);
        } else if (e instanceof VenueCancelDone c) {
            handleCancel(envelope);
        } else {
            log.warn("Unknown venueId event type: {}", e);
        }
    }

    /* ====== Handlers ====== */

    private void handleAck(Envelope<VenueEvent> envelope) {
        VenueAck event = (VenueAck) envelope.payload();
        Meta meta = envelope.meta();
        metaFactory.addHop(meta, Stage.SOR_RECV_ACK);
        SorEffects eff = stateStore.apply(
                event.venueId(), event.venueOrderId(), event.childClOrdId(), event,
                (state, intent) -> childVenueReducer.reduce(state, event)
        );
        for (OrderEvent ev : eff.orderEvents()) {
            sorEvtBus.offer(Envelope.of(ev, meta));
        }
    }

    private void handleFill(Envelope<VenueEvent> envelope) {
        VenueFill event = (VenueFill) envelope.payload();
        Meta meta = envelope.meta();
        metaFactory.addHop(meta, Stage.SOR_RECV_FILLED);
        SorEffects eff = stateStore.apply(
                event.venueId(), event.venueOrderId(), event.childClOrdId(), event,
                (state, intent) -> childVenueReducer.reduce(state, event)
        );
        for (OrderEvent ev : eff.orderEvents()) {
            sorEvtBus.offer(Envelope.of(ev, meta));
        }
    }

    private void handleCancel(Envelope<VenueEvent> envelope) {
        Meta meta = envelope.meta();
        VenueCancelDone event = (VenueCancelDone) envelope.payload();
        metaFactory.addHop(meta, Stage.SOR_RECV_CANCEL);
        SorEffects eff = stateStore.apply(
                event.venueId(), event.venueOrderId(), event.childClOrdId(), event,
                (state, intent) -> childVenueReducer.reduce(state, event)
        );

        for (OrderEvent ev : eff.orderEvents()) {
            sorEvtBus.offer(Envelope.of(ev, meta));
        }
    }
}
