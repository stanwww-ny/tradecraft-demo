package io.tradecraft.venue.listener;

import io.tradecraft.common.envelope.Envelope;
import io.tradecraft.common.envelope.EnvelopeMetaFactory;
import io.tradecraft.oms.event.EventQueue;
import io.tradecraft.venue.event.VenueEvent;
import io.tradecraft.venue.event.VenueFill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TinyVenueListener implements VenueListener {
    private static final Logger log = LoggerFactory.getLogger(TinyVenueListener.class);
    private final EventQueue<Envelope<VenueEvent>> venueEvtBus; // internal
    private final EnvelopeMetaFactory metaFactory;

    public TinyVenueListener(EventQueue<Envelope<VenueEvent>> venueEvtBus, EnvelopeMetaFactory metaFactory) {
        this.venueEvtBus = venueEvtBus;
        this.metaFactory = metaFactory;
    }

    @Override
    public void onEvent(Envelope<VenueEvent> envelope) {
        VenueEvent v = envelope.payload();
        if (v instanceof VenueFill fill) {
            if (fill.lastQty() <= 0) {
                log.warn("Ignoring zero-qty VenueFill {}", fill);
                return;
            }
        }
        venueEvtBus.offer(envelope);
    }

//    /*
//
//        log.info("VENUE->LISTENER, {}", ack);
//        ParentOrderId parentId = new ParentOrderId(ack.parentId());
//        ChildOrderId childId = new ChildOrderId((ack.childId()));
//        long tsNanos = ack.tsNanos();
//        String venueOrderId = ack.childId();
//        if (ack.lastQty() <= 0 || ack.cumQty() <= 0 || ack.lastPxMicros() <= 0) {
//            throw new IllegalArgumentException("Invalid fill ack: " + ack);
//        }
//        switch (ack.kind()) {
//            case ACK -> evtBus.offer(
//                    new EvChildAck(parentId, tsNanos, childId, venueOrderId));
//            case REJECT -> evtBus.offer(
//                    new EvChildReject(parentId, tsNanos, childId, "VENUE", ack.rejectReason()));
//            case PARTIAL_FILL -> evtBus.offer(
//                    new EvChildFill(
//                            parentId, tsNanos,
//                            childId,
//                            ack.lastQty(),
//                            ack.lastPxMicros(),
//                            ack.cumQty(),
//                            /* execId */ ack.childId() + "-exec",
//                            /* venueId */ "N/A",
//                            false
//                    ));
//            case FILL -> evtBus.offer(
//                    new EvChildFill(
//                            parentId, tsNanos,
//                            childId,
//                            ack.lastQty(),
//                            ack.lastPxMicros(),
//                            ack.cumQty(),
//                            /* execId */ ack.childId() + "-exec",
//                            /* venueId */ "N/A",
//                            true
//                    ));
//            case CANCEL_ACK -> evtBus.offer(
//                    new EvChildCanceled(
//                            parentId, tsNanos,
//                            childId, ack.rejectReason()));
//            case CANCEL_REJECT -> evtBus.offer(
//                    new EvError(parentId, tsNanos, "CANCEL_REJECT: " + ack.rejectReason()));
//        }
//
//    @Override
//    public void on(VenueAck v) {
//        evBus.off
//    }
//
//    @Override
//    public void on(VenueFill v) {
//
//    }
//
//    @Override
//    public void on(VenueCancel v) {
//
//    }
//
//    @Override
//    public void on(VenueReject v) {
//
//    }
//        */
}
