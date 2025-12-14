package io.tradecraft.venue.store;

import io.tradecraft.common.domain.order.CancelReason;
import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.VenueId;
import io.tradecraft.common.id.VenueOrderId;
import io.tradecraft.venue.cmd.NewChildCmd;
import io.tradecraft.venue.matching.orderbook.FillSource;
import io.tradecraft.venue.matching.orderbook.RestingRef;
import io.tradecraft.venue.model.VenueOrder;

import java.util.Optional;

// io.tradecraft.venueId.store
public interface VenueOrderRepository {

    // Creation & lookup
    VenueOrder create(NewChildCmd cmd, VenueId venueId, VenueOrderId venueOrderId);     // alloc VenueOrderId, leaves=orig

    Optional<VenueOrder> get(ChildId childId);

    Optional<VenueOrder> byVenue(VenueOrderId voi);

    // Life-cycle / bookkeeping
    void ack(VenueOrder vo, long tsNanos);

    // centralized math + state transitions
    void applyFill(VenueOrder vo, long lastQty, long lastPxMicros, boolean finalFlag, FillSource src);

    void markResting(VenueOrder vo, RestingRef ref);

    void clearResting(VenueOrder vo);

    void cancel(VenueOrder vo, CancelReason reason);

    void applyReplace(VenueOrder vo, long newQty, @javax.annotation.Nullable Long newLimitPxMicros);

    // Idempotency guards (venueId-level truth)
    boolean seenCmd(String cmdId);   // true iff first time seen

    boolean seenExec(String execId); // true iff first time seen
}
