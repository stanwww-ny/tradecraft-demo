package io.tradecraft.venue.api;

import io.tradecraft.common.domain.order.CancelReason;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.event.VenueAck;
import io.tradecraft.venue.event.VenueCancelDone;
import io.tradecraft.venue.event.VenueFill;
import io.tradecraft.venue.matching.orderbook.FillSource;
import io.tradecraft.venue.model.VenueOrder;

public interface VenueEmitter {
    VenueAck ack(NewChildCmd c, VenueOrder vo);

    VenueFill applyFill(VenueOrder vo, long lastQty, long lastPxMicros,
                        boolean finalFlag, FillSource source);

    VenueCancelDone cancel(VenueOrder vo, CancelReason reason);

    VenueCancelDone cancel(VenueOrder vo, long leaves, CancelReason reason);
}
